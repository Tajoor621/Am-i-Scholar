"use strict";
const KEY="amischolar.web.v2",OLD="amischolar.web.v1";
const MODULES=[
["topic-lab","Topic Lab","Refine research questions","◈","Write a focused, feasible and answerable research question."],
["literature-radar","Literature Radar","Document literature searches","⌕","Capture real publications, their DOI/URL, authors and relevance."],
["evidence-matrix","Evidence Matrix","Compare evidence transparently","▤","Record method, sample, finding, limitation and source for each study."],
["outline-architect","Outline Architect","Build a structured manuscript","☷","Organize Introduction, Methods, Results, Discussion and References."],
["draft-studio","Draft Studio","Write and refine your paper","✎","Create manuscript drafts and inspect basic word counts."],
["style-coach","Style Coach","Review writing clarity","✧","Find long sentences, passive-language signals and readability issues."],
["citation-center","Citation Center","Audit source metadata","❞","Inspect DOI, author, year and journal completeness before formatting."],
["integrity","Originality & Integrity","Review academic integrity","✓","Check attribution, source verification and research disclosures."],
["review-simulator","Review Simulator","Prepare for peer review","◇","Use an explicit simulated reviewer checklist, not fabricated reviews."],
["submission-kit","Submission Kit","Plan journal submission","⇧","Verify journal guidelines, scope, fees, ethics and required files."],
["revision-response","Revision & Response","Track revision differences","↺","Compare your last two revision records and prepare responses."],
["archive","Archive & Reproducibility","Record reproducible methods","▣","Document software, datasets, parameters, versions and recovery."]
];
const $=id=>document.getElementById(id);
const uid=()=>typeof crypto!=="undefined"&&crypto.randomUUID?crypto.randomUUID():Date.now().toString(36)+Math.random().toString(36).slice(2);
const safeText=(v,max=2000000)=>typeof v==="string"?v.slice(0,max):"";
let data;
try{data=JSON.parse(localStorage.getItem(KEY)||localStorage.getItem(OLD)||'{"version":2,"projects":[],"records":[]}');if(!Array.isArray(data.projects)||!Array.isArray(data.records))throw Error();data.version=2;if(!Array.isArray(data.evidenceLinks))data.evidenceLinks=[]}catch{data={version:2,projects:[],records:[],evidenceLinks:[]}}
let projectId=null,moduleId=null,editId=null,view="home",noticeTimeout;
function save(){try{localStorage.setItem(KEY,JSON.stringify(data))}catch(e){alert("Browser storage is full or unavailable. Export a backup now. "+e.message);throw e}renderStats()}
function notify(t){$("status").textContent=t;clearTimeout(noticeTimeout);noticeTimeout=setTimeout(()=>$("status").textContent="",5500)}
function el(tag,content="",className=""){const e=document.createElement(tag);e.textContent=content;e.className=className;return e}
function button(label,action,kind="secondary"){const b=el("button",label,kind);b.type="button";b.onclick=action;return b}
function project(){return data.projects.find(p=>p.id===projectId)}
function recordsFor(id=projectId,module=moduleId){return data.records.filter(r=>r.projectId===id&&(!module||r.moduleId===module)).sort((a,b)=>(b.updated||0)-(a.updated||0))}
function section(name){["home","projects","modules","library","settings","projectForm","workspace"].forEach(k=>$(k+"Page").classList.toggle("hidden",k!==name));view=name;document.querySelectorAll("aside [data-nav]").forEach(b=>b.classList.toggle("active",b.dataset.nav===name));window.scrollTo({top:0,behavior:"instant"})}
function navigate(name){if(name==="home"){renderHome()}else if(name==="projects"){renderProjects()}else if(name==="modules"){renderModules()}else if(name==="library"){renderLibrary()}else if(name==="settings"){section("settings")}else section(name);$("pageTitle").textContent=({home:"Dashboard",projects:"My papers",modules:"Research modules",library:"Source library",settings:"Settings",projectForm:"Research paper",workspace:"Research workspace"})[name]||"Dashboard"}
document.querySelectorAll("[data-nav]").forEach(b=>b.onclick=()=>navigate(b.dataset.nav));
function renderStats(){const all=data.records;const sources=all.filter(r=>r.moduleId==="literature-radar"||r.moduleId==="citation-center");$("countProjects").textContent=data.projects.length;$("countSources").textContent=sources.length;$("countDrafts").textContent=all.filter(r=>["draft-studio","outline-architect"].includes(r.moduleId)).length;$("countRecords").textContent=all.length}
function moduleCard(m){const c=el("article","","panel module");c.append(el("div",m[3],"icon"),el("h3",m[1]),el("p",m[2]),button("Open "+m[1],()=>openModule(m[0])));return c}
function fillModules(target){target.replaceChildren(...MODULES.map(moduleCard))}
function renderHome(){navigateTitle("Dashboard");section("home");renderStats();fillModules($("homeModules"));const list=$("recentProjects");list.replaceChildren();if(!data.projects.length){const e=el("div","","empty");e.append(el("div","▤","icon"),el("h3","Your research journey starts here"),el("p","Create a paper to save a research question, collect sources and work through all 12 tools."),button("Create your first paper",newPaper));list.append(e)}else data.projects.slice(-3).reverse().forEach(p=>list.append(projectCard(p)))}
function navigateTitle(t){$("pageTitle").textContent=t}
function projectCard(p){const c=el("article","","record");const row=el("div","","row");const info=el("div");info.append(el("h3",p.title),el("small",p.discipline||"Research paper"));row.append(info,button("Open paper",()=>{projectId=p.id;renderModules()}));c.append(row);if(p.question)c.append(el("p",p.question));return c}
function renderProjects(){section("projects");navigateTitle("My papers");const list=$("projectList");list.replaceChildren();if(!data.projects.length)list.append(el("div","No papers yet. Create one to get started.","empty"));data.projects.slice().reverse().forEach(p=>list.append(projectCard(p)))}
function renderModules(){section("modules");navigateTitle("Research modules");fillModules($("allModules"))}
function renderLibrary(){section("library");navigateTitle("Source library");const list=$("sourceList");list.replaceChildren();const refs=data.records.filter(r=>["literature-radar","citation-center"].includes(r.moduleId));if(!refs.length)list.append(el("p","No source records yet. Open Literature Radar to add one.","empty"));refs.forEach(r=>{const p=data.projects.find(p=>p.id===r.projectId);const c=el("div","","record");c.append(el("h3",r.title),el("small",p?.title||"Unknown project"),el("p",r.content.slice(0,500)),button("Open record",()=>{projectId=r.projectId;moduleId=r.moduleId;openWorkspace()}));list.append(c)})}
function newPaper(){projectId=null;$("projectForm").reset();$("projectFormTitle").textContent="New research paper";section("projectForm");navigateTitle("New research paper")}
function editPaper(){const p=project();if(!p)return newPaper();$("projectTitle").value=p.title;$("projectQuestion").value=p.question||"";$("projectDiscipline").value=p.discipline||"";$("projectCitation").value=p.citationStyle||"APA";$("projectFormTitle").textContent="Edit research paper";section("projectForm")}
function choosePaper(onChoose){if(!data.projects.length){notify("Create a paper first to use this research module.");newPaper();return}if(data.projects.length===1){projectId=data.projects[0].id;onChoose();return}const names=data.projects.map((p,i)=>(i+1)+". "+p.title).join("\n");const n=prompt("Choose a paper number:\n"+names);if(n===null)return;const i=Number(n)-1;if(Number.isInteger(i)&&data.projects[i]){projectId=data.projects[i].id;onChoose()}else notify("Select a valid paper number.")}
function openModule(id){moduleId=id;if(project())openWorkspace();else choosePaper(openWorkspace)}
function openWorkspace(){const p=project(),m=MODULES.find(m=>m[0]===moduleId);if(!p||!m)return renderModules();section("workspace");navigateTitle(m[1]);$("workspacePaper").textContent=p.title;$("workspaceTitle").textContent=m[1];$("workspaceDescription").textContent=m[4];editId=null;$("recordForm").reset();renderTool();renderRecords()}
function renderRecords(){const list=$("recordList"),records=recordsFor();list.replaceChildren();$("recordCount").textContent=records.length+" records";if(!records.length)list.append(el("div","No records yet. Use the research template above or create your first record.","empty"));records.forEach(r=>{const c=el("article","","record");const top=el("div","","row");top.append(el("h3",r.title),el("small",new Date(r.updated||Date.now()).toLocaleString()));c.append(top,el("p",r.content.slice(0,1200)));const actions=el("div","","toolbar");actions.append(button("Edit",()=>{editId=r.id;$("recordTitle").value=r.title;$("recordContent").value=r.content;$("recordTitle").focus()}),button("Delete",()=>{if(confirm("Permanently delete this record?")){data.records=data.records.filter(x=>x.id!==r.id);data.evidenceLinks=data.evidenceLinks.filter(l=>l.claimId!==r.id&&l.sourceId!==r.id);save();renderRecords();renderTool()}},"danger"));c.append(actions);list.append(c)})}
function addTemplate(title,content){editId=null;$("recordTitle").value=title;$("recordContent").value=content;$("recordTitle").focus();notify("Template loaded. Complete and save it as your own research record.")}
function toolLine(parent,text){parent.append(el("p",text))}
function checklist(parent,items){const ul=el("ul");items.forEach(t=>ul.append(el("li",t)));parent.append(ul)}
function analysisBox(parent,title,items){const box=el("div","","feature");box.append(el("h3",title));checklist(box,items.length?items:["No issues detected by these basic checks. Verify manually."]);parent.append(box)}
function parseFields(text){const out={};text.split("\n").forEach(line=>{const i=line.indexOf(":");if(i>0)out[line.slice(0,i).trim().toLowerCase()]=line.slice(i+1).trim()});return out}
function renderLiteratureSearch(){
 const root=$("liveSearch");root.classList.remove("hidden");root.className="panel";root.style.margin="12px 0";
 root.append(el("h3","Live scholarly literature search"));
 const label=el("label"),title=el("span","Keywords, title or DOI");const input=document.createElement("input");input.id="literatureQuery";input.placeholder="e.g. bacterial chromosome organization";input.maxLength=200;label.append(title,input);root.append(label);
 const row=el("div","","toolbar"),select=document.createElement("select");select.id="literatureProvider";select.style.width="auto";
 for(const [value,name] of [["crossref","Crossref"],["openalex","OpenAlex"]]){const opt=document.createElement("option");opt.value=value;opt.textContent=name;select.append(opt)}
 const results=el("div","","list");results.id="literatureResults";row.append(select,button("Search research metadata",async()=>{
 results.replaceChildren(el("p","Searching provider…"));const q=input.value.trim();
 try{const items=await LiteratureAPI.search(q,select.value);results.replaceChildren();if(!items.length)results.append(el("p","No matching metadata. Try different keywords."));
 items.forEach(item=>{const card=el("article","","record");card.append(el("h3",item.title),el("p",[item.authors,item.year,item.journal].filter(Boolean).join(" · ")),el("small",item.doi?"DOI: "+item.doi:"No DOI in metadata"),button("Import metadata",()=>{
 const entry=LiteratureAPI.record(item);const exists=data.records.some(r=>r.projectId===projectId&&r.moduleId==="literature-radar"&&r.content.includes("https://doi.org/"+item.doi)&&item.doi);
 if(exists){notify("This DOI already exists in this paper.");return}
 data.records.push({id:uid(),projectId,moduleId:"literature-radar",title:entry.title,content:entry.content,updated:Date.now()});save();renderRecords();notify("Source imported as unverified metadata.");
 }));results.append(card)})}catch(e){results.replaceChildren(el("p","Search unavailable: "+e.message+". Check connectivity or provider CORS policy."))}}, "secondary"));root.append(row,results);
}
function renderModuleHelp(){
 const box=$("moduleHelp"),help=ScholarHelp.help[moduleId];if(!help)return;
 box.replaceChildren();
 const row=el("div","","row");row.append(el("h2","How to use "+MODULES.find(m=>m[0]===moduleId)?.[1]),button("Close help",()=>box.classList.add("hidden"),"secondary"));box.append(row);
 box.append(el("p",help.goal));
 const heading=el("h3","Step-by-step procedure"),list=document.createElement("ol");help.steps.forEach(step=>list.append(el("li",step)));box.append(heading,list);
 box.append(el("h3","Practical example"),el("p",help.example));
 box.append(el("h3","Important accuracy notes"));const warnings=document.createElement("ul");help.cautions.forEach(t=>warnings.append(el("li",t)));box.append(warnings);
 box.append(el("h3","Suggestions for your current research"));const tips=document.createElement("ul");ScholarHelp.feedback(moduleId,project(),recordsFor()).forEach(t=>tips.append(el("li",t)));box.append(tips);
 box.append(el("p","Suggestions are based on visible project fields and saved records, not external fact checking or scientific peer review.","muted"));
 box.classList.remove("hidden");box.scrollIntoView({behavior:"smooth",block:"start"});
}
function exportEvidenceMatrix(format){
 const rows=recordsFor(projectId,"evidence-matrix");
 const fields=["Claim","Support status","Source citation / DOI","Research design","Sample / setting","Main findings (quote or paraphrase with page)","Limitations and potential bias","Relevance to question","Confidence and justification"];
 const dataRows=rows.map(r=>{const f=parseFields(r.content);return [f.claim||r.title,f["support status"]||"unassessed",...fields.slice(2).map(k=>f[k.toLowerCase()]||"")]});
 const warning="Researcher-authored evidence matrix. Verify all sources, quotations, claims and interpretations against originals.";
 if(format==="csv"){const csv=[["Record title",...fields].join(","),...rows.map((r,i)=>[r.title,...dataRows[i]].map(v=>'"'+String(v).replace(/"/g,'""')+'"').join(","))].join("\n");download("am-i-scholar-evidence.csv",csv,"text/csv");return}
 const md="# Evidence matrix\n\n> "+warning+"\n\n| "+fields.join(" | ")+" |\n|"+fields.map(()=>"---").join("|")+"|\n"+dataRows.map(row=>"| "+row.map(v=>String(v).replace(/\|/g,"\\|").replace(/\n/g," ")).join(" | ")+" |").join("\n");
 download("am-i-scholar-evidence.md",md,"text/markdown");
}
function renderEvidenceLinks(panel,actions){
 const claims=recordsFor(projectId,"evidence-matrix"),sources=recordsFor(projectId,null).filter(r=>["literature-radar","citation-center"].includes(r.moduleId));
 const wrap=el("div","","feature");wrap.append(el("h3","Persistent claim-to-source relationships"),el("p","A relationship records your assessment, not independent verification. Link only sources you have checked."));
 const form=document.createElement("form"),claim=document.createElement("select"),source=document.createElement("select"),status=document.createElement("select"),rationale=document.createElement("textarea");
 const addOptions=(target,items)=>items.forEach(x=>{const o=document.createElement("option");o.value=x.id;o.textContent=x.title;target.append(o)});
 addOptions(claim,claims);addOptions(source,sources);
 for(const [value,label] of [["supported","Supported (researcher assessed)"],["partially supported","Partially supported"],["unsupported","Unsupported"]]){const o=document.createElement("option");o.value=value;o.textContent=label;status.append(o)}
 const labeled=(label,input)=>{const l=el("label");l.append(el("span",label),input);form.append(l)};
 labeled("Evidence/claim record",claim);labeled("Source record",source);labeled("Assessment",status);labeled("Rationale and exact location in source",rationale);
 const submit=el("button","Save evidence link");submit.type="submit";submit.disabled=!claims.length||!sources.length;form.append(submit);
 form.onsubmit=e=>{e.preventDefault();if(!claim.value||!source.value)return;data.evidenceLinks.push({id:uid(),projectId,claimId:claim.value,sourceId:source.value,status:status.value,rationale:rationale.value.trim(),createdAt:new Date().toISOString()});save();renderTool()};
 wrap.append(form);if(!claims.length||!sources.length)wrap.append(el("p","Create an evidence record and a source record first."));
 const links=(data.evidenceLinks||[]).filter(l=>l.projectId===projectId);
 for(const link of links){const c=claims.find(r=>r.id===link.claimId),src=sources.find(r=>r.id===link.sourceId);if(!c||!src)continue;
 const entry=el("article","","record");entry.append(el("h3",c.title+" → "+src.title),el("p",link.status+" · "+(link.rationale||"No rationale entered")));
 entry.append(button("Remove relationship",()=>{if(!confirm("Remove this claim-to-source relationship?"))return;data.evidenceLinks=data.evidenceLinks.filter(l=>l.id!==link.id);save();renderTool()},"danger"));wrap.append(entry)}
 if(links.some(l=>l.status==="unsupported"))wrap.append(el("p","⚠ Unsupported claims are present. Review them before export.","notice"));
 panel.append(wrap);
}
function renderTool(){const panel=$("toolPanel"),actions=$("toolActions"),p=project(),rows=recordsFor();panel.replaceChildren();actions.replaceChildren();$("moduleHelp").classList.add("hidden");actions.append(button("❔ Help & research guidance",renderModuleHelp,"secondary"));$("liveSearch").classList.add("hidden");$("liveSearch").replaceChildren();const template=(title,content)=>actions.append(button("Use structured template",()=>addTemplate(title,content)));
switch(moduleId){
case "topic-lab":{toolLine(panel,"Research question: "+(p.question||"Not set — edit your paper to add one."));const q=(p.question||"").trim();analysisBox(panel,"Question quality checks",[!q?"Missing research question":null,q&&q.split(/\s+/).length<8?"Question may be too broad or short":null,q&&!/[?]$/.test(q)?"Consider phrasing the question explicitly":null,!/\b(how|what|why|whether|which|does|do|is|are|can|to what extent)\b/i.test(q)?"Specify a measurable outcome or explanatory relationship":null].filter(Boolean));actions.append(button("Edit research question",editPaper));template("Feasibility assessment","Population or system:\nResearch gap:\nVariables / outcomes:\nMethodology:\nData access:\nEthics considerations:\nTimeline and constraints:\nFeasibility decision:");break}
case "literature-radar":{toolLine(panel,"Search live scholarly metadata through Crossref or OpenAlex when online. Results are metadata only: verify original papers before using them.");renderLiteratureSearch();template("Source title — author and year","Authors:\nYear:\nDOI or official URL:\nJournal / repository:\nSearch terms:\nDate accessed:\nAbstract summary (your own words):\nRelevance:\nVerified against original: No");analysisBox(panel,"Search planning",["Define keywords and synonyms","Record inclusion and exclusion criteria","Check publication date and study type","Verify every DOI against the original publication"]);break}
case "evidence-matrix":{template("Evidence from verified study","Claim:\nSupport status:\nSource citation / DOI:\nResearch design:\nSample / setting:\nMain findings (quote or paraphrase with page):\nLimitations and potential bias:\nRelevance to question:\nConfidence and justification:");const issues=[];rows.forEach(r=>{const f=parseFields(r.content);if(!f["source citation / doi"])issues.push(r.title+": missing source identifier");if(!f["limitations and potential bias"])issues.push(r.title+": limitations missing");if(!f["main findings (quote or paraphrase with page)"])issues.push(r.title+": finding missing")});rows.forEach(r=>{const f=parseFields(r.content);if(!["supported","partially supported","unsupported"].includes((f["support status (supported / partially supported / unsupported)"]||f["support status"]||"").toLowerCase()))issues.push(r.title+": explicitly assess support status");if(/unsupported/i.test(f["support status"]||f["support status (supported / partially supported / unsupported)"]||""))issues.push(r.title+": UNSUPPORTED CLAIM — revise or add valid evidence")});analysisBox(panel,"Evidence traceability",issues.length?issues:["No missing fields found; manually verify claims and sources."]);actions.append(button("Export evidence CSV",()=>exportEvidenceMatrix("csv")));actions.append(button("Export evidence Markdown",()=>exportEvidenceMatrix("markdown")));renderEvidenceLinks(panel,actions);break}
case "outline-architect":{template("Manuscript outline","Introduction:\nResearch gap:\nResearch question:\nMethods:\nResults / expected analysis:\nDiscussion:\nLimitations:\nConclusion:\nReferences and source links:");const issues=[];rows.forEach(r=>["introduction","method","conclusion","reference"].forEach(k=>{if(!r.content.toLowerCase().includes(k))issues.push(r.title+": consider adding "+k)}));analysisBox(panel,"Outline coverage",issues);break}
case "draft-studio":{const count=rows.reduce((n,r)=>n+r.content.trim().split(/\s+/).filter(Boolean).length,0);toolLine(panel,"Draft word count across saved records: "+count);template("Manuscript draft","Abstract:\nIntroduction:\nMethods:\nResults:\nDiscussion:\nLimitations:\nConclusion:\nReferences:\n");actions.append(button("Export draft text",()=>download("am-i-scholar-draft.txt",rows.map(r=>r.title+"\n"+r.content).join("\n\n"),"text/plain")));actions.append(button("Export Markdown",()=>exportManuscript("markdown")));actions.append(button("Export print-ready HTML",()=>exportManuscript("html")));actions.append(button("Print / Save PDF",()=>exportManuscript("print")));break}
case "style-coach":{template("Clarity revision","Original paragraph:\nRevised paragraph:\nWhy this improves clarity:\nTerminology / factual checks:");const drafts=data.records.filter(r=>r.projectId===projectId&&r.moduleId==="draft-studio");const long=drafts.flatMap(r=>r.content.split(/[.!?]+/).filter(s=>s.trim().split(/\s+/).length>30).map(s=>r.title+": "+s.trim().slice(0,110)));analysisBox(panel,"Offline writing signals",drafts.length?long.length?long:["No sentences over 30 words detected. This is not a full grammar check."]:["No draft found. Add text in Draft Studio first."]);break}
case "citation-center":{toolLine(panel,"Draft citation — verify against original source and target journal requirements. Local CSL styles do not validate your metadata.");template("Bibliographic record","Authors:\nYear:\nTitle:\nJournal / publisher:\nVolume / issue:\nPages:\nDOI or official URL:\nVerified against original: No");const issues=[];rows.forEach(r=>{const f=parseFields(r.content);["authors","year","doi or official url"].forEach(k=>{if(!f[k])issues.push(r.title+": missing "+k)})});analysisBox(panel,"Reference metadata audit",issues.length?issues:["No missing required fields in saved citation records. Verify metadata against the original before submitting."]);actions.append(button("Export CSL-JSON metadata",exportCslJson));CitationWorkspace.render(panel,recordsFor(projectId,"citation-center").concat(recordsFor(projectId,"literature-radar")),parseFields,download);break}
case "integrity":{template("Integrity and ethics review","Claims matched to sources:\nDirect quotations and page numbers:\nParaphrase attribution:\nHuman/animal ethics approval if applicable:\nConsent if applicable:\nConflicts of interest:\nFunding disclosure:\nAI-use disclosure per policy:\nData integrity and limitations:");analysisBox(panel,"Human review required",["No plagiarism database or similarity score is provided","Check citations and quotations against originals","Follow institution and journal disclosure policies","Verify ethical approvals before submission"]);break}
case "review-simulator":{template("Simulated reviewer response","Comment / potential concern:\nEvidence supporting response:\nResponse to reviewer:\nManuscript change:\nSection / line numbers:\nUnresolved issue:");analysisBox(panel,"Simulated reviewer checklist",["Is the research question novel and justified?","Can another researcher reproduce the method?","Do the findings support each conclusion?","Are limitations and biases clearly disclosed?","Are all sources verifiable?"]);break}
case "submission-kit":{template("Target journal requirements","Target journal:\nOfficial HTTPS author-guidelines URL:\nJournal scope and indexing:\nPublication fees verified:\nWord and figure limits:\nReporting checklist:\nEthics / consent:\nData availability statement:\nCover letter:\nSubmission readiness:");const issues=[];rows.forEach(r=>{const f=parseFields(r.content);if(!f["target journal"])issues.push(r.title+": journal missing");if(!/^https:\/\//i.test(f["official https author-guidelines url"]||""))issues.push(r.title+": official HTTPS guidelines link missing")});analysisBox(panel,"Journal due diligence",issues.length?issues:["Verify journal scope, editorial board, indexing and fees on official websites. No automated journal recommendation."]);break}
case "revision-response":{template("Revision record","Version:\nDate:\nReviewer comment:\nAuthor response:\nChanges made:\nManuscript excerpt:\nUnresolved items:");if(rows.length>1){const [latest,prior]=rows;const a=new Set(prior.content.split("\n").filter(Boolean)),b=new Set(latest.content.split("\n").filter(Boolean));analysisBox(panel,"Line-level changes: "+prior.title+" → "+latest.title,[...b].filter(x=>!a.has(x)).slice(0,6).map(x=>"+ "+x).concat([...a].filter(x=>!b.has(x)).slice(0,6).map(x=>"− "+x)))}else toolLine(panel,"Save two revision records to compare added and removed lines.");break}
case "archive":{template("Reproducibility record","Version and date:\nStudy protocol:\nData location and access permissions:\nSoftware and versions:\nParameters / environment:\nAnalysis steps:\nEthics and privacy considerations:\nLimitations:\nReproduction checklist:");toolLine(panel,"This project contains "+recordsFor(projectId,null).length+" local records. Export a portable backup in Settings.");actions.append(button("Export this paper for Android",()=>exportAndroid(projectId)));break}
}}
function exportManuscript(format){
 const p=project();if(!p)return;
 const notes=recordsFor(projectId,"draft-studio");
 const title=p.title||"Research manuscript";
 const text=notes.map(n=>n.title+"\n\n"+n.content).join("\n\n");
 if(format==="markdown"){download("am-i-scholar-manuscript.md","# "+title+"\n\n"+text,"text/markdown");return}
 if(format==="html"){
  const escape=t=>t.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
  download("am-i-scholar-manuscript.html",'<!doctype html><html lang="en"><meta charset="utf-8"><title>'+escape(title)+'</title><style>body{max-width:800px;margin:48px auto;font:12pt/1.6 Georgia,serif;padding:16px}pre{white-space:pre-wrap;font:inherit}</style><h1>'+escape(title)+'</h1><pre>'+escape(text)+'</pre></html>',"text/html");return
 }
 if(format==="print"){
  const win=window.open("","_blank");if(!win){notify("Allow pop-ups to print the manuscript.");return}
  const pre=win.document.createElement("pre");pre.textContent=title+"\n\n"+text;pre.style.cssText="white-space:pre-wrap;font:12pt/1.6 Georgia,serif;margin:40px";win.document.body.append(pre);win.document.title=title;win.focus();win.print();return
 }
}
function exportCslJson(){
 const p=project();if(!p)return;
 const rows=recordsFor(projectId,"citation-center").concat(recordsFor(projectId,"literature-radar"));
 const entries=rows.map(r=>{
  const f=parseFields(r.content),authors=(f.authors||"").split(";").map(n=>n.trim()).filter(Boolean).map(name=>({literal:name}));
  const year=Number.parseInt(f.year,10),raw=f["doi or official url"]||f["doi or url"]||"",doi=LiteratureAPI.normalizeDOI(raw);
  const item={id:r.id,type:"article-journal",title:f.title||r.title};
  if(authors.length)item.author=authors;
  if(Number.isInteger(year)&&year>0&&year<3000)item.issued={"date-parts":[[year]]};
  if(f["journal / publisher"])item["container-title"]=f["journal / publisher"];
  if(/^10\.\d{4,9}\//i.test(doi))item.DOI=doi;
  if(/^https?:\/\//i.test(raw))item.URL=raw;
  return item;
 });
 download("am-i-scholar-csl-data.json",JSON.stringify(entries,null,2));
 notify("CSL-JSON metadata exported with stable record IDs. Citation rendering remains a draft until independently CSL-tested.");
}
function download(name,content,mime="application/json"){const blob=new Blob([content],{type:mime}),url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
function webBackup(){download("am-i-scholar-web-backup.json",JSON.stringify({format:"am-i-scholar-web",version:2,projects:data.projects,records:data.records,evidenceLinks:data.evidenceLinks},null,2));notify("Backup exported. Store it somewhere safe.")}
function androidSnapshot(id){
 const p=data.projects.find(p=>p.id===id);if(!p)throw Error("Paper not found");
 const notes=[],sources=[],noteIds=[],sourceIds=[];
 const kinds={"topic-lab":"TOPIC","evidence-matrix":"EVIDENCE","outline-architect":"OUTLINE","draft-studio":"DRAFT","style-coach":"STYLE","integrity":"INTEGRITY","review-simulator":"REVIEW","submission-kit":"SUBMISSION","revision-response":"REVISION","archive":"ARCHIVE"};
 for(const r of recordsFor(id,null)){
  if(["literature-radar","citation-center"].includes(r.moduleId)){
   const f=parseFields(r.content);sourceIds.push(r.id);
   sources.push({title:r.title,authors:f.authors||"",year:f.year||"",doiOrUrl:f["doi or official url"]||f["doi or url"]||"",publication:f["journal / publisher"]||"",evidenceNotes:r.content,verified:/^yes$/i.test(f["verified against original"]||"")});
  }else{noteIds.push(r.id);notes.push({title:r.title,content:r.content,kind:kinds[r.moduleId]||"NOTE"})}
 }
 const evidenceLinks=(data.evidenceLinks||[]).filter(l=>l.projectId===id).map(l=>({
  noteIndex:noteIds.indexOf(l.claimId),sourceIndex:sourceIds.indexOf(l.sourceId),status:l.status,rationale:l.rationale||""
 })).filter(l=>l.noteIndex>=0&&l.sourceIndex>=0&&notes[l.noteIndex].kind==="EVIDENCE");
 return {format:"am-i-scholar-project",version:1,project:{title:p.title,researchQuestion:p.question||"",discipline:p.discipline||"",citationStyle:p.citationStyle||"APA"},notes,sources,evidenceLinks}
}
function exportAndroid(id){const snapshot=androidSnapshot(id);download("am-i-scholar-android-project.json",JSON.stringify(snapshot,null,2));notify("Android-compatible project backup exported.")}
function importBackup(obj){if(!obj||typeof obj!=="object")throw Error("Invalid JSON");if(obj.format==="am-i-scholar-web"&&(obj.version===1||obj.version===2)){if(!Array.isArray(obj.projects)||!Array.isArray(obj.records)||obj.projects.length+obj.records.length+(Array.isArray(obj.evidenceLinks)?obj.evidenceLinks.length:0)>10000)throw Error("Invalid web backup");for(const p of obj.projects)if(typeof p.id!=="string"||typeof p.title!=="string")throw Error("Invalid project");for(const r of obj.records)if(typeof r.id!=="string"||typeof r.projectId!=="string"||typeof r.moduleId!=="string"||typeof r.title!=="string"||typeof r.content!=="string")throw Error("Invalid record");if(!confirm("Replace ALL current browser research with this web backup? Export your current data first."))return;data={version:2,projects:obj.projects,records:obj.records,evidenceLinks:Array.isArray(obj.evidenceLinks)?obj.evidenceLinks.filter(l=>l&&typeof l.id==="string"&&typeof l.claimId==="string"&&typeof l.sourceId==="string"&&["supported","partially supported","unsupported"].includes(l.status)):[]};save();renderHome();notify("Web backup restored.");return}
if(obj.format==="am-i-scholar-project"&&obj.version===1){const p=obj.project;if(!p||typeof p.title!=="string"||!Array.isArray(obj.notes)||!Array.isArray(obj.sources)||obj.notes.length+obj.sources.length+(Array.isArray(obj.evidenceLinks)?obj.evidenceLinks.length:0)>10000)throw Error("Invalid Android project backup");if(!confirm("Import this Android research project as a NEW browser paper?"))return;const id=uid(),next={id,title:safeText(p.title,200),question:safeText(p.researchQuestion),discipline:safeText(p.discipline),citationStyle:safeText(p.citationStyle)||"APA"};if(!next.title.trim())throw Error("Empty project title");const imported=[],noteIds=[],sourceIds=[];const kinds={TOPIC:"topic-lab",EVIDENCE:"evidence-matrix",OUTLINE:"outline-architect",DRAFT:"draft-studio",STYLE:"style-coach",INTEGRITY:"integrity",REVIEW:"review-simulator",SUBMISSION:"submission-kit",REVISION:"revision-response",ARCHIVE:"archive"};for(const n of obj.notes){if(typeof n.title!=="string"||typeof n.content!=="string")throw Error("Invalid note");const record={id:uid(),projectId:id,moduleId:kinds[n.kind]||"draft-studio",title:safeText(n.title),content:safeText(n.content),updated:Date.now()};noteIds.push(record.id);imported.push(record)}for(const s of obj.sources){if(typeof s.title!=="string")throw Error("Invalid source");const record={id:uid(),projectId:id,moduleId:"literature-radar",title:safeText(s.title),content:"Authors: "+safeText(s.authors)+"\nYear: "+safeText(s.year)+"\nDOI or official URL: "+safeText(s.doiOrUrl)+"\nJournal / publisher: "+safeText(s.publication)+"\nVerified against original: "+(s.verified?"Yes":"No")+"\nEvidence notes: "+safeText(s.evidenceNotes),updated:Date.now()};sourceIds.push(record.id);imported.push(record)}const links=[];for(const l of obj.evidenceLinks||[]){if(!Number.isInteger(l.noteIndex)||!Number.isInteger(l.sourceIndex)||!noteIds[l.noteIndex]||!sourceIds[l.sourceIndex]||obj.notes[l.noteIndex]?.kind!=="EVIDENCE"||!["supported","partially supported","unsupported"].includes(l.status)||typeof l.rationale!=="string")throw Error("Invalid evidence relationship in backup");links.push({id:uid(),projectId:id,claimId:noteIds[l.noteIndex],sourceId:sourceIds[l.sourceIndex],status:l.status,rationale:safeText(l.rationale),createdAt:new Date().toISOString()})}data.evidenceLinks=data.evidenceLinks||[];data.evidenceLinks.push(...links);data.projects.push(next);data.records.push(...imported);save();projectId=id;renderModules();notify("Android project imported as a new browser paper.");return}
throw Error("Unsupported backup format or version")}
$("projectForm").onsubmit=e=>{e.preventDefault();const title=$("projectTitle").value.trim();if(!title)return;let p=project();if(!p){p={id:uid()};data.projects.push(p)}p.title=title;p.question=$("projectQuestion").value;p.discipline=$("projectDiscipline").value;p.citationStyle=$("projectCitation").value;projectId=p.id;save();renderModules();notify("Research paper saved.")};
$("recordForm").onsubmit=e=>{e.preventDefault();const title=$("recordTitle").value.trim(),content=$("recordContent").value.trim();if(!title||!content)return;let r=data.records.find(r=>r.id===editId);if(!r){r={id:uid(),projectId,moduleId};data.records.push(r)}r.title=title;r.content=content;r.updated=Date.now();save();editId=null;$("recordForm").reset();renderRecords();renderTool();notify("Record saved on this device.")};
$("clearRecord").onclick=()=>{editId=null;$("recordForm").reset()};
$("headerNew").onclick=$("heroNew").onclick=$("projectNew").onclick=newPaper;
$("cancelProject").onclick=()=>project()?renderModules():renderProjects();
$("backWorkspace").onclick=renderModules;$("editPaper").onclick=editPaper;$("switchPaper").onclick=renderProjects;
$("backupWeb").onclick=webBackup;$("backupAndroid").onclick=()=>choosePaper(()=>exportAndroid(projectId));
$("restoreFile").onchange=async e=>{const file=e.target.files?.[0];if(!file)return;try{if(file.size>10*1024*1024)throw Error("File exceeds 10 MB");importBackup(JSON.parse(await file.text()))}catch(err){notify("Restore failed: "+err.message)}finally{e.target.value=""}};
function connection(){const online=navigator.onLine;$("connection").textContent=online?"● Online":"● Offline";$("connection").style.background=online?"#e3f6ed":"#fff0d9"}
window.addEventListener("online",connection);window.addEventListener("offline",connection);connection();
if("serviceWorker" in navigator)window.addEventListener("load",()=>navigator.serviceWorker.register("./sw.js").catch(()=>notify("Offline installation unavailable in this browser.")));
renderHome();