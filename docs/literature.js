"use strict";
/* Explicit, user-initiated public metadata search. Never silently transmits local research data. */
const LiteratureAPI=(()=>{
 const clean=v=>String(v??"").replace(/<[^>]*>/g,"").trim().slice(0,2000);
 const normalizeDOI=v=>clean(v).replace(/^https?:\/\/(?:dx\.)?doi\.org\//i,"").replace(/^doi:/i,"").trim().toLowerCase();
 const year=v=>String(v||"");
 const authorList=a=>(a||[]).map(x=>typeof x==="string"?x:[x.given,x.family].filter(Boolean).join(" ")).filter(Boolean).join("; ");
 const provenance=(x,provider)=>({...x,provider,fetchedAt:new Date().toISOString(),verified:false});
 const crossref=x=>provenance({title:clean(x.title?.[0]),authors:authorList(x.author),year:year(x.published?.["date-parts"]?.[0]?.[0]||x.issued?.["date-parts"]?.[0]?.[0]),doi:normalizeDOI(x.DOI),journal:clean(x["container-title"]?.[0]),url:clean(x.URL),abstract:clean(x.abstract)},"Crossref");
 const openalex=x=>provenance({title:clean(x.display_name),authors:(x.authorships||[]).map(a=>a.author?.display_name).filter(Boolean).join("; "),year:year(x.publication_year),doi:normalizeDOI(x.doi),journal:clean(x.primary_location?.source?.display_name),url:clean(x.primary_location?.landing_page_url||x.id),abstract:""},"OpenAlex");
 const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
 async function json(url){
  for(let attempt=0;attempt<3;attempt++){
   const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),12000);
   try{
    const response=await fetch(url,{signal:controller.signal,headers:{Accept:"application/json"},redirect:"error"});
    if(response.status===429||response.status>=500){
     if(attempt===2)throw Error(response.status===429?"Provider rate limit reached. Please retry later.":"Provider temporarily unavailable.");
     const retry=Number(response.headers.get("Retry-After"));
     await delay(Number.isFinite(retry)&&retry>0?Math.min(retry*1000,5000):500*(2**attempt));continue;
    }
    if(!response.ok)throw Error("Provider returned HTTP "+response.status);
    return await response.json();
   }catch(e){if(attempt===2||!["AbortError","TypeError"].includes(e.name))throw e;await delay(500*(2**attempt))}
   finally{clearTimeout(timer)}
  }
 }
 function dedupe(items){const seen=new Set();return items.filter(x=>{if(!x.title)return false;const key=x.doi?"doi:"+x.doi:"meta:"+x.title.toLocaleLowerCase().replace(/[^\p{L}\p{N}]/gu,"")+"|"+x.year+"|"+(x.authors.split(";")[0]||"").toLocaleLowerCase();if(seen.has(key))return false;seen.add(key);return true})}
 async function search(query,provider="crossref",limit=8){
  const q=query.trim();if(q.length<3||q.length>200)throw Error("Enter 3–200 characters to search.");
  if(!navigator.onLine)throw Error("Offline: manual source entry remains available.");
  const n=Math.max(1,Math.min(limit,20));let records;
  if(provider==="crossref"){const x=await json("https://api.crossref.org/works?query="+encodeURIComponent(q)+"&rows="+n);records=(x.message?.items||[]).map(crossref)}
  else if(provider==="openalex"){const x=await json("https://api.openalex.org/works?search="+encodeURIComponent(q)+"&per-page="+n);records=(x.results||[]).map(openalex)}
  else throw Error("Provider not supported in the web app yet.");
  return dedupe(records);
 }
 function record(item){return {title:item.title,content:"Authors: "+item.authors+"\nYear: "+item.year+"\nDOI or official URL: "+(item.doi?"https://doi.org/"+item.doi:item.url)+"\nJournal / publisher: "+item.journal+"\nProvider: "+item.provider+"\nLast fetched (UTC): "+(item.fetchedAt||"Unknown")+"\nAbstract / summary: "+item.abstract+"\nVerified against original: No\nRelevance:\nLimitations:"}}
 return {search,record,normalizeDOI,dedupe};
})();
