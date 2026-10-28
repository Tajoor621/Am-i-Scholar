"use strict";
/* User-initiated public metadata search. Imported records remain local and unverified. */
const LiteratureAPI=(()=>{
 const clean=v=>String(v??"").replace(/<[^>]*>/g," ").replace(/\s+/g," ").trim().slice(0,4000);
 const normalizeDOI=v=>clean(v).replace(/^https?:\/\/(?:dx\.)?doi\.org\//i,"").replace(/^doi:/i,"").replace(/[.,;]+$/g,"").trim().toLowerCase();
 const year=v=>String(v||"");
 const authorList=a=>(a||[]).map(x=>typeof x==="string"?x:[x.given,x.family].filter(Boolean).join(" ")).filter(Boolean).join("; ");
 const provenance=(x,provider)=>({...x,doi:normalizeDOI(x.doi),provider,providers:[provider],oaStatus:x.oaStatus||"unknown",oaUrl:x.oaUrl||"",fetchedAt:new Date().toISOString(),verified:false});
 const crossref=x=>provenance({title:clean(x.title?.[0]),authors:authorList(x.author),year:year(x.published?.["date-parts"]?.[0]?.[0]||x.issued?.["date-parts"]?.[0]?.[0]),doi:x.DOI,journal:clean(x["container-title"]?.[0]),url:clean(x.URL),abstract:clean(x.abstract)},"Crossref");
 function invertedAbstract(index){if(!index||typeof index!=="object")return "";const words=[];for(const [word,positions] of Object.entries(index))for(const pos of positions||[])words[pos]=word;return words.filter(Boolean).join(" ")}
 const openalex=x=>{const oa=x.open_access||{},location=oa.best_oa_location||{};return provenance({title:clean(x.display_name),authors:(x.authorships||[]).map(a=>a.author?.display_name).filter(Boolean).join("; "),year:year(x.publication_year),doi:x.doi,journal:clean(x.primary_location?.source?.display_name),url:clean(x.primary_location?.landing_page_url||x.id),abstract:clean(x.abstract||invertedAbstract(x.abstract_inverted_index)),oaStatus:oa.is_oa?(oa.oa_status||"open access"):(oa.is_oa===false?"closed":"unknown"),oaUrl:clean(location.pdf_url||location.landing_page_url)},"OpenAlex")};
 function parsePubMed(xml){
  const doc=new DOMParser().parseFromString(xml,"application/xml");
  if(doc.querySelector("parsererror"))throw Error("PubMed returned invalid XML.");
  return [...doc.querySelectorAll("PubmedArticle")].map(article=>{
   const text=selector=>article.querySelector(selector)?.textContent?.trim()||"";
   const authors=[...article.querySelectorAll("AuthorList Author")].map(a=>[a.querySelector("ForeName")?.textContent,a.querySelector("LastName")?.textContent].filter(Boolean).join(" ")||a.querySelector("CollectiveName")?.textContent).filter(Boolean).join("; ");
   const doi=[...article.querySelectorAll("ArticleId[IdType='doi'], ELocationID[EIdType='doi']")].map(x=>x.textContent).find(Boolean)||"";
   const abstract=[...article.querySelectorAll("Abstract AbstractText")].map(x=>{const label=x.getAttribute("Label");return (label?label+": ":"")+x.textContent.trim()}).filter(Boolean).join(" ");
   const date=text("PubDate Year")||text("ArticleDate Year")||text("PubDate MedlineDate").match(/\d{4}/)?.[0]||"";
   const pmid=text("PMID");
   return provenance({title:clean(text("ArticleTitle")),authors,year:year(date),doi,journal:clean(text("Journal Title")||text("Journal ISOAbbreviation")),url:pmid?"https://pubmed.ncbi.nlm.nih.gov/"+pmid+"/":"",abstract:clean(abstract)},"PubMed");
  }).filter(x=>x.title);
 }
 const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
 async function request(url,format="json"){
  for(let attempt=0;attempt<3;attempt++){
   const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),12000);
   try{
    const response=await fetch(url,{signal:controller.signal,headers:{Accept:format==="json"?"application/json":"application/xml, text/xml"},redirect:"error"});
    if(response.status===429||response.status>=500){if(attempt===2)throw Error(response.status===429?"Provider rate limit reached. Please retry later.":"Provider temporarily unavailable.");const retry=Number(response.headers.get("Retry-After"));await delay(Number.isFinite(retry)&&retry>0?Math.min(retry*1000,5000):500*(2**attempt));continue}
    if(!response.ok)throw Error("Provider returned HTTP "+response.status);
    return format==="json"?await response.json():await response.text();
   }catch(e){if(attempt===2||!(["AbortError","TypeError"].includes(e.name)))throw e;await delay(500*(2**attempt))}
   finally{clearTimeout(timer)}
  }
 }
 let ncbiQueue=Promise.resolve(),lastNcbiRequest=0;
 function ncbi(url,format){ncbiQueue=ncbiQueue.catch(()=>{}).then(async()=>{const wait=Math.max(0,350-(Date.now()-lastNcbiRequest));if(wait)await delay(wait);lastNcbiRequest=Date.now();return request(url,format)});return ncbiQueue}
 async function pubmedSearch(query,limit,email){
  const params=new URLSearchParams({db:"pubmed",term:query,retmax:String(limit),retmode:"json",tool:"amischolar"});if(email)params.set("email",email);
  const found=await ncbi("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?"+params,"json"),ids=found.esearchresult?.idlist||[];
  if(!ids.length)return [];
  const fetchParams=new URLSearchParams({db:"pubmed",id:ids.join(","),retmode:"xml",tool:"amischolar"});if(email)fetchParams.set("email",email);
  return parsePubMed(await ncbi("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?"+fetchParams,"xml"));
 }
 async function searchProvider(query,provider,limit,email){
  if(provider==="crossref"){
   const p=new URLSearchParams({query,rows:String(limit)});if(email)p.set("mailto",email);
   const data=await request("https://api.crossref.org/works?"+p);return (data.message?.items||[]).map(crossref);
  }
  if(provider==="openalex"){
   const p=new URLSearchParams({search:query,"per-page":String(limit)});if(email)p.set("mailto",email);
   const data=await request("https://api.openalex.org/works?"+p);return (data.results||[]).map(openalex);
  }
  if(provider==="pubmed")return pubmedSearch(query,limit,email);
  throw Error("Provider not supported in the web app.");
 }
 function mergeRecord(target,incoming){
  const merged={...target};
  for(const key of ["title","authors","year","journal","doi","url","abstract","oaStatus","oaUrl"])if((!merged[key]||merged[key]==="unknown")&&incoming[key])merged[key]=incoming[key];
  merged.providers=[...new Set([...(target.providers||[target.provider]),...(incoming.providers||[incoming.provider])].filter(Boolean))];
  if(merged.oaStatus==="unknown"&&incoming.oaStatus!=="unknown")merged.oaStatus=incoming.oaStatus;
  if(!merged.oaUrl&&incoming.oaUrl)merged.oaUrl=incoming.oaUrl;
  return merged;
 }
 function dedupe(items){
  const out=[],index=new Map();
  for(const x of items){if(!x.title)continue;const title=x.title.toLocaleLowerCase().replace(/[^\p{L}\p{N}]/gu,""),first=(x.authors.split(";")[0]||"").toLocaleLowerCase();const key=x.doi?"doi:"+normalizeDOI(x.doi):"meta:"+title+"|"+x.year+"|"+first;if(index.has(key)){const i=index.get(key);out[i]=mergeRecord(out[i],x)}else{index.set(key,out.length);out.push(x)}}
  return out;
 }
 async function unpaywall(item,email){
  if(!email||!item.doi)return item;
  try{const data=await request("https://api.unpaywall.org/v2/"+encodeURIComponent(item.doi)+"?email="+encodeURIComponent(email));const loc=data.best_oa_location||{};return {...item,oaStatus:data.is_oa?(data.oa_status||"open access"):(item.oaStatus==="unknown"?"closed":item.oaStatus),oaUrl:clean(loc.url_for_pdf||loc.url)||item.oaUrl}}
  catch{return item}
 }
 async function enrichOpenAccess(records,email){
  if(!email)return records;
  const enriched=new Array(records.length);let next=0;
  const worker=async()=>{while(next<records.length){const i=next++;enriched[i]=await unpaywall(records[i],email)}};
  await Promise.all(Array.from({length:Math.min(3,records.length)},worker));return enriched;
 }
 async function search(query,provider="all",limit=20,email=""){
  const q=String(query||"").trim();if(q.length<3||q.length>200)throw Error("Enter 3–200 characters to search.");
  if(!navigator.onLine)throw Error("Offline: manual source entry remains available.");
  const n=Math.max(1,Math.min(Number(limit)||20,20));const providers=provider==="all"?["crossref","openalex","pubmed"]:[provider];
  const settled=await Promise.allSettled(providers.map(p=>searchProvider(q,p,n,email)));
  const records=dedupe(settled.flatMap(r=>r.status==="fulfilled"?r.value:[]));
  const errors=settled.flatMap((r,i)=>r.status==="rejected"?[{provider:providers[i],message:r.reason?.message||"Search failed."}]:[]);
  const enriched=await enrichOpenAccess(records,email);
  return {records:dedupe(enriched),errors};
 }
 function record(item,status="to-read",notes=""){
  return {title:item.title,content:"Authors: "+item.authors+"\nYear: "+item.year+"\nDOI or official URL: "+(item.doi?"https://doi.org/"+item.doi:item.url)+"\nJournal / publisher: "+item.journal+"\nProvider: "+(item.providers||[item.provider]).join(", ")+"\nAbstract / summary: "+(item.abstract||"")+"\nOpen access: "+(item.oaStatus||"unknown")+"\nOpen access URL: "+(item.oaUrl||"")+"\nStatus: "+status+"\nNotes: "+notes+"\nLast fetched (UTC): "+(item.fetchedAt||"Unknown")+"\nVerified against original: No\nRelevance:\nLimitations:"};
 }
 return {search,record,normalizeDOI,dedupe,parsePubMed};
})();
