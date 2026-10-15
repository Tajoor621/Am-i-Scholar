"use strict";
/** Public scholarly metadata, user-initiated requests only. No API keys or paid services. */
const LiteratureAPI=(()=>{
 const timeout=12000;
 async function json(url){
  const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),timeout);
  try{const response=await fetch(url,{signal:controller.signal,headers:{Accept:"application/json"},redirect:"error"});
   if(!response.ok)throw Error("Provider returned HTTP "+response.status);
   return await response.json();
  }finally{clearTimeout(timer)}
 }
 function clean(v){return String(v??"").replace(/<[^>]*>/g,"").trim().slice(0,2000)}
 function normalizeDOI(v){return clean(v).replace(/^https?:\/\/(?:dx\.)?doi\.org\//i,"").replace(/^doi:/i,"").trim()}
 function crossref(item){
  return {provider:"Crossref",title:clean(item.title?.[0]),authors:(item.author||[]).map(a=>[a.given,a.family].filter(Boolean).join(" ")).join("; "),year:String(item.published?.["date-parts"]?.[0]?.[0]||item.issued?.["date-parts"]?.[0]?.[0]||""),doi:normalizeDOI(item.DOI),journal:clean(item["container-title"]?.[0]),url:clean(item.URL),abstract:clean(item.abstract)};
 }
 function openalex(item){
  return {provider:"OpenAlex",title:clean(item.display_name),authors:(item.authorships||[]).map(a=>a.author?.display_name).filter(Boolean).join("; "),year:String(item.publication_year||""),doi:normalizeDOI(item.doi),journal:clean(item.primary_location?.source?.display_name),url:clean(item.primary_location?.landing_page_url||item.id),abstract:""};
 }
 async function search(query,provider="crossref",limit=8){
  const q=query.trim();if(q.length<3||q.length>200)throw Error("Enter 3–200 characters to search.");
  if(!navigator.onLine)throw Error("You are offline. Saved sources remain available.");
  const n=Math.max(1,Math.min(limit,20));
  if(provider==="crossref"){const result=await json("https://api.crossref.org/works?query="+encodeURIComponent(q)+"&rows="+n);return (result.message?.items||[]).map(crossref).filter(x=>x.title)}
  if(provider==="openalex"){const result=await json("https://api.openalex.org/works?search="+encodeURIComponent(q)+"&per-page="+n);return (result.results||[]).map(openalex).filter(x=>x.title)}
  throw Error("Unsupported research provider");
 }
 function record(item){return {title:item.title,content:"Authors: "+item.authors+"\nYear: "+item.year+"\nDOI or official URL: "+(item.doi?"https://doi.org/"+item.doi:item.url)+"\nJournal / publisher: "+item.journal+"\nProvider: "+item.provider+"\nAbstract / summary: "+item.abstract+"\nVerified against original: No\nRelevance:\nLimitations:"}}
 return {search,record,normalizeDOI};
})();
