"use strict";
(function(root){
 const MAX_SOURCES=5,MAX_PROMPT_CHARS=8000,MAX_TASK_CHARS=600;
 const clean=value=>typeof value==="string"?value.replace(/\u0000/g,"").trim():"";
 const clip=(value,max)=>{const text=clean(value);return {text:text.length>max?text.slice(0,max):text,truncated:text.length>max}};
 function buildPrompt(task,selected){
  const chosen=Array.isArray(selected)?selected.slice(0,MAX_SOURCES):[];
  let truncated=Array.isArray(selected)&&selected.length>MAX_SOURCES;
  const taskClip=clip(task,MAX_TASK_CHARS);truncated=truncated||taskClip.truncated;
  const pre="You are assisting with academic research. Use only the explicitly selected source records below. Do not invent facts, findings, quotations, citations, DOIs, or bibliographic details. Separate source-supported statements from suggestions. Mark claims not supported by these records as unsupported and state uncertainty. Preserve each source label [S#] when citing it.\n\nTask:\n"+(taskClip.text||"Summarize the selected evidence and identify limitations.")+"\n\nSelected source records:\n";
  const post="\n\nReturn a concise synthesis, a limitations section, and a list of unsupported questions. Do not claim that you verified the publications.";
  const sourceIds=chosen.map(x=>clean(x?.id)).filter(Boolean);
  const available=Math.max(0,MAX_PROMPT_CHARS-pre.length-post.length);
  const blocks=[];
  let remaining=available;
  chosen.forEach((item,index)=>{
   const id=clean(item?.id)||"missing-id";
   const title=clip(item?.title,240);const identifier=clip(item?.identifier,320);
   const evidence=clip(item?.evidence,1200);
   truncated=truncated||title.truncated||identifier.truncated||evidence.truncated;
   const header="[S"+(index+1)+"] Local source ID: "+id+"\nTitle: "+(title.text||"Untitled record")+"\nDOI/URL: "+(identifier.text||"[none recorded; do not invent one]")+"\nVerification: "+(item?.verified===true?"researcher marked verified":"UNVERIFIED")+".\n";
   const evidencePrefix="Selected evidence notes: ";
   if(header.length>remaining){truncated=true;return}
   let block=header;remaining-=header.length;
   if(evidence.text&&remaining>evidencePrefix.length+1){
    const body=evidence.text.slice(0,Math.max(0,remaining-evidencePrefix.length-1));
    if(body.length<evidence.text.length)truncated=true;
    block+=evidencePrefix+body+"\n";remaining-=evidencePrefix.length+body.length+1;
   }else if(evidence.text)truncated=true;
   blocks.push(block);
  });
  let prompt=pre+blocks.join("\n")+post;
  if(prompt.length>MAX_PROMPT_CHARS){prompt=prompt.slice(0,MAX_PROMPT_CHARS-post.length)+post;truncated=true}
  return {prompt,sourceIds:sourceIds.slice(0,MAX_SOURCES),truncated,omittedSources:Math.max(0,chosen.length-blocks.length)};
 }
 function canCopy(consent,result){return consent===true&&!!result&&Array.isArray(result.sourceIds)&&result.sourceIds.length>0&&result.prompt.length<=MAX_PROMPT_CHARS}
 function disclosure({provider,task,sourceIds,reviewed}={}){
  const name=clean(provider)||"an external AI service";
  const work=clean(task)||"research drafting";
  const ids=Array.isArray(sourceIds)?sourceIds.map(clean).filter(Boolean):[];
  return "AI assistance disclosure: I used "+name+" to assist with "+work+". "+(ids.length?"The handoff included selected local source record IDs: "+ids.join(", ")+". ":"")+"I "+(reviewed?"reviewed the returned text and checked its claims and citations against original sources.":"have not yet verified the returned text or its citations.")+" The app did not verify the AI output.";
 }
 root.AiHandoff={MAX_SOURCES,MAX_PROMPT_CHARS,MAX_TASK_CHARS,buildPrompt,canCopy,disclosure};
})(typeof window!=="undefined"?window:globalThis);
