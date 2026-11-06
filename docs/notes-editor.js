"use strict";
const StudentNotesEditor=(()=>{
 const MAX_NOTE_CHARS=2000000,MAX_IMAGE_BYTES=320000,MAX_IMAGE_PIXELS=1400,FENCE=String.fromCharCode(96).repeat(3);
 const el=(tag,text="",className="")=>{const node=document.createElement(tag);node.textContent=text;node.className=className;return node};
 function appendInline(parent,text){
  const pattern=new RegExp("(\\\\*\\\\*[^*]+\\\\*\\\\*|==[^=]+==|"+FENCE+"[^"+FENCE+"]+"+FENCE+")","g");let cursor=0;
  for(const match of String(text).matchAll(pattern)){parent.append(document.createTextNode(text.slice(cursor,match.index)));const token=match[0],isCode=token.startsWith(FENCE),inner=token.slice(isCode?3:2,isCode?-3:-2),node=el(token.startsWith("**")?"strong":token.startsWith("==")?"mark":"code",inner);parent.append(node);cursor=match.index+token.length}
  parent.append(document.createTextNode(String(text).slice(cursor)))
 }
 function renderPreview(host,source){
  host.replaceChildren();const lines=String(source||"").split(/\\r?\\n/);let list=null,listType="",codeLines=null;
  const flushList=()=>{list=null;listType=""};
  for(const line of lines){
   if(line.trim().startsWith(FENCE)){flushList();if(codeLines){const pre=el("pre");pre.append(el("code",codeLines.join("\\n")));host.append(pre);codeLines=null}else codeLines=[];continue}
   if(codeLines){codeLines.push(line);continue}
   const image=line.match(/^!\\[([^\\]]*)\\]\\((data:image\\/(?:jpeg|png|webp);base64,[A-Za-z0-9+/=]+)\\)$/);
   if(image){flushList();const figure=el("figure","","student-note-image"),img=document.createElement("img");img.alt=image[1]||"Note image";img.src=image[2];img.loading="lazy";figure.append(img);host.append(figure);continue}
   const unordered=line.match(/^\\s*[-*]\\s+(.*)$/),ordered=line.match(/^\\s*\\d+[.)]\\s+(.*)$/);
   if(unordered||ordered){const kind=unordered?"ul":"ol";if(!list||listType!==kind){flushList();list=el(kind);listType=kind;host.append(list)}const item=el("li");appendInline(item,(unordered||ordered)[1]);list.append(item);continue}
   flushList();if(!line.trim()){host.append(el("p",""));continue}
   const heading=line.match(/^(#{1,3})\\s+(.*)$/),block=heading?el("h"+heading[1].length):el("p");appendInline(block,heading?heading[2]:line);host.append(block)
  }
  if(codeLines){const pre=el("pre");pre.append(el("code",codeLines.join("\\n")));host.append(pre)}
 }
 function insert(textarea,before,after="",fallback="Text"){
  const start=textarea.selectionStart??textarea.value.length,end=textarea.selectionEnd??textarea.value.length,selected=textarea.value.slice(start,end)||fallback;
  textarea.setRangeText(before+selected+after,start,end,"select");textarea.focus();textarea.dispatchEvent(new Event("input",{bubbles:true}))
 }
 async function imageData(file){
  if(!file||!file.type.startsWith("image/"))throw Error("Choose an image file.");
  if(file.size>12*1024*1024)throw Error("Choose an image smaller than 12 MB.");
  const url=URL.createObjectURL(file);
  try{const image=new Image();image.src=url;await image.decode();const scale=Math.min(1,MAX_IMAGE_PIXELS/Math.max(image.width,image.height)),canvas=document.createElement("canvas");canvas.width=Math.max(1,Math.round(image.width*scale));canvas.height=Math.max(1,Math.round(image.height*scale));canvas.getContext("2d").drawImage(image,0,0,canvas.width,canvas.height);const data=canvas.toDataURL("image/jpeg",.72);if(data.length>MAX_IMAGE_BYTES*1.4)throw Error("This image is too detailed for a portable note. Choose a smaller image.");return data}finally{URL.revokeObjectURL(url)}
 }
 function enhance(textarea){
  if(!textarea||textarea.dataset.richNotes==="true")return;
  textarea.dataset.richNotes="true";textarea.setAttribute("aria-describedby",((textarea.getAttribute("aria-describedby")||"")+" studentNoteEditorHelp").trim());
  const wrapper=el("div","","student-note-editor"),toolbar=el("div","","student-note-toolbar"),preview=el("div","","student-note-preview"),status=el("p","","muted");
  toolbar.setAttribute("role","toolbar");toolbar.setAttribute("aria-label","Note formatting");status.id="studentNoteEditorStatus";status.setAttribute("role","status");status.setAttribute("aria-live","polite");
  const help=el("p","Markdown source is saved locally. Images are resized before being embedded; PDF text is extracted on this device.","muted");help.id="studentNoteEditorHelp";
  const commands=[["Bold","**","**","bold text"],["Highlight","==","==","highlighted text"],["Bulleted list","- ","","list item"],["Numbered list","1. ","","list item"],["Code block","\\n"+FENCE+"\\n","\\n"+FENCE+"\\n","code"]];
  for(const [label,before,after,fallback] of commands){const control=el("button",label,"secondary");control.type="button";control.onclick=()=>insert(textarea,before,after,fallback);toolbar.append(control)}
  const imageInput=document.createElement("input");imageInput.type="file";imageInput.accept="image/*";imageInput.hidden=true;imageInput.setAttribute("aria-label","Choose an image to insert");
  const imageButton=el("button","Add image","secondary");imageButton.type="button";imageButton.onclick=()=>imageInput.click();
  imageInput.onchange=async()=>{try{const file=imageInput.files?.[0],data=await imageData(file),count=(textarea.value.match(/!\\[[^\\]]*\\]\\(data:image\\//g)||[]).length;if(count>=4)throw Error("A note can contain up to four images.");const name=(file.name||"image").replace(/[()[\\]]/g," ");insert(textarea,"!["+name+"]("+data+")","","image");status.textContent="Image resized and added to this note."}catch(error){status.textContent=error.message||"Could not add image."}finally{imageInput.value=""}};
  const pdfInput=document.createElement("input");pdfInput.type="file";pdfInput.accept=".pdf,application/pdf";pdfInput.hidden=true;pdfInput.setAttribute("aria-label","Choose a PDF to extract");
  const pdfButton=el("button","Import PDF text","secondary");pdfButton.type="button";pdfButton.onclick=()=>pdfInput.click();
  pdfInput.onchange=async()=>{const file=pdfInput.files?.[0];if(!file)return;if(typeof PDFReader==="undefined"||typeof PDFReader.extractText!=="function"){status.textContent="PDF text extraction is unavailable.";pdfInput.value="";return}pdfButton.disabled=true;status.textContent="Extracting PDF text locally…";try{const extracted=await PDFReader.extractText(file),combined=(textarea.value?textarea.value+"\\n\\n":"")+"## "+file.name+"\\n\\n"+extracted;if(combined.length>MAX_NOTE_CHARS)throw Error("Extracted text is too long for one note. Split the PDF into smaller parts.");textarea.value=combined;textarea.dispatchEvent(new Event("input",{bubbles:true}));status.textContent="PDF text added locally. Check it before using it."}catch(error){status.textContent="PDF import failed: "+(error.message||"could not read the file")}finally{pdfButton.disabled=false;pdfInput.value=""}};
  toolbar.append(imageButton,imageInput,pdfButton,pdfInput);
  const label=textarea.closest("label");if(label)label.after(wrapper);else textarea.after(wrapper);
  wrapper.append(toolbar,help,preview,status);const update=()=>renderPreview(preview,textarea.value);textarea.addEventListener("input",update);update()
 }
 return {enhance,renderPreview};
})();
window.StudentNotesEditor=StudentNotesEditor;