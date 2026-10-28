"use strict";
/* Offline CSL engine adapter. Bundled citeproc-js + version-pinned CSL styles and en-US locale.
   Formatting is standards-based; incomplete source metadata still requires human review. */
const CitationWorkspace = (() => {
 const styles = [
  ["apa","APA 7th"],["modern-language-association","MLA 9th"],
  ["chicago-author-date","Chicago author-date"],["chicago-notes-bibliography-17th-edition","Chicago notes (17th)"],
  ["ieee","IEEE"],["harvard-cite-them-right","Harvard (Cite Them Right)"],["elsevier-vancouver","Vancouver (Elsevier/NLM variant)"]
 ];
 const cache = new Map();
 async function asset(url) {
  if (!cache.has(url)) cache.set(url,fetch(url).then(r => {if(!r.ok)throw Error("Offline CSL asset unavailable: "+url);return r.text()}).catch(e=>{cache.delete(url);throw e}));
  return cache.get(url);
 }
 function cslItem(r, parse) {
  const f=parse(r.content);if(f["csl-json"]){try{const original=JSON.parse(f["csl-json"]);if(original&&typeof original==="object"&&original.title)return {...original,id:r.id}}catch{}}const authorText=(f.authors||"").split(";").map(x=>x.trim()).filter(Boolean);
  const authors=authorText.map(name=>{const match=name.match(/^(.+?),\s*(.+)$/);return match?{family:match[1],given:match[2]}:{literal:name}});
  const y=Number.parseInt(f.year,10);
  const raw=f["doi or official url"]||f["doi or url"]||"";
  const doi=raw.replace(/^https?:\/\/(?:dx\.)?doi\.org\//i,"").replace(/^doi:/i,"").trim();
  const item={id:r.id,type:"article-journal",title:f.title||r.title};
  if(authors.length)item.author=authors;
  if(Number.isInteger(y)&&y>0&&y<3000)item.issued={"date-parts":[[y]]};
  if(f["journal / publisher"])item["container-title"]=f["journal / publisher"];
  if(f["volume / issue"]){const v=f["volume / issue"].split(/[(),]/);if(v[0]?.trim())item.volume=v[0].trim();if(v[1]?.trim())item.issue=v[1].trim()}
  if(f.pages)item.page=f.pages;
  if(/^10\.\d{4,9}\//i.test(doi))item.DOI=doi;
  if(/^https?:\/\//i.test(raw))item.URL=raw;
  return item;
 }
 function safeRich(html){
  const source=new DOMParser().parseFromString("<div>"+html+"</div>","text/html").body.firstElementChild;
  const allowed=new Set(["I","B","EM","STRONG","SUP","SUB","SPAN","DIV","BR","SMALL"]);
  const out=document.createElement("div");
  function append(from,to){for(const node of from.childNodes){if(node.nodeType===3){to.append(document.createTextNode(node.textContent));continue}if(node.nodeType!==1)continue;const next=document.createElement(allowed.has(node.tagName)?node.tagName.toLowerCase():"span");to.append(next);append(node,next)}}
  append(source,out);return out;
 }
 async function bibliography(items,style){
  if(typeof CSL==="undefined")throw Error("CSL engine did not load.");
  const [xml,locale]=await Promise.all([asset("./vendor/styles/"+style+".csl"),asset("./vendor/locales-en-US.xml")]);
  const byId=Object.fromEntries(items.map(item=>[item.id,item]));
  const sys={retrieveLocale:()=>locale,retrieveItem:id=>byId[id]};
  const engine=new CSL.Engine(sys,xml,"en-US");
  engine.setOutputFormat("html");
  engine.updateItems(items.map(x=>x.id));
  const result=engine.makeBibliography();
  if(!result)return [];
  return result[1];
 }
 function metadataWarnings(items){return items.map(item=>{const missing=[];if(!Array.isArray(item.author)||!item.author.length)missing.push("author");const year=item.issued?.["date-parts"]?.[0]?.[0];if(!year)missing.push("year");if(!item.title)missing.push("title");if(item.type==="article-journal"&&!item["container-title"])missing.push("journal");if(["book","chapter"].includes(item.type)&&!item.publisher)missing.push("publisher");return {title:item.title||"Untitled source",missing}}).filter(x=>x.missing.length)}
 function render(root,records,parse,download){
  root.replaceChildren();
  const box=document.createElement("div");box.className="feature";
  const heading=document.createElement("h3");heading.textContent="Offline CSL bibliography";
  const notice=document.createElement("p");notice.textContent="Draft citation — verify against original source and target journal requirements. Formatting uses locally bundled CSL styles; metadata is not independently verified.";
  const warnings=document.createElement("div");warnings.id="citationWarnings";warnings.className="feature";warnings.setAttribute("aria-label","Citation metadata warnings");warnings.append(Object.assign(document.createElement("h4"),{textContent:"Metadata completeness warnings"}));
  const label=document.createElement("label");label.textContent="Citation style";
  const select=document.createElement("select");select.id="cslStyle";
  for(const [value,title] of styles){const o=document.createElement("option");o.value=value;o.textContent=title;select.append(o)}
  label.append(select);
  const btn=document.createElement("button");btn.type="button";btn.textContent="Format bibliography offline";
  const exportBtn=document.createElement("button");exportBtn.type="button";exportBtn.textContent="Export CSL bibliography Markdown";exportBtn.disabled=true;
  const result=document.createElement("div");result.id="cslResult";result.setAttribute("aria-live","polite");
  const items=records.map(r=>cslItem(r,parse));let output=[];const gaps=metadataWarnings(items);if(!items.length)warnings.append(Object.assign(document.createElement("p"),{textContent:"Add source records to check their metadata."}));else if(!gaps.length)warnings.append(Object.assign(document.createElement("p"),{textContent:"No basic metadata gaps detected. Verify all fields against the original source."}));else gaps.forEach(entry=>{const p=document.createElement("p");p.textContent=entry.title+": "+entry.missing.map(x=>"Missing "+x).join(" · ");warnings.append(p)});
  btn.onclick=async()=>{btn.disabled=true;exportBtn.disabled=true;result.textContent="Formatting locally…";
   try{output=await bibliography(items,select.value);result.replaceChildren();
    if(!output.length)result.textContent="No references available. Add source records first.";
    else output.forEach(entry=>{const div=safeRich(entry);div.className="record";result.append(div)});
    exportBtn.disabled=!output.length;
   }catch(e){result.textContent="CSL formatting unavailable: "+e.message+" — your research records are unchanged."}
   finally{btn.disabled=false}
  };
  exportBtn.onclick=()=>{if(!output.length)return;const txt="# Bibliography\n\n> Draft citations: verify against original sources and journal requirements.\n\n"+output.map(x=>safeRich(x).textContent.trim()).join("\n\n");download("am-i-scholar-csl-bibliography.md",txt,"text/markdown")};
  const toolbar=document.createElement("div");toolbar.className="toolbar";toolbar.append(btn,exportBtn);
  box.append(heading,notice,warnings,label,toolbar,result);root.append(box);
  if(!items.length)result.textContent="Add a source in Citation Center or Literature Radar to format a bibliography.";
 }
 return {styles,cslItem,bibliography,metadataWarnings,render};
})();
