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
function renderLibrary(){section("library");navigateTitle("Source library");const list=$("sourceList");list.replaceChildren();const refs=data.records.filter(r=>["literature-radar","citation-center"].includes(r.moduleId));if(!refs.length)list.append(el("p","No source records yet. Open Literature Radar to add one.","empty"));refs.forEach(r=>{const p=data.projects.find(p=>p.id===r.projectId),fields=parseFields(r.content),meta=[fields.status&&"Status: "+fields.status,fields.verified&&"Verified: "+fields.verified].filter(Boolean).join(" · ");const c=el("div","","record");c.append(el("h3",r.title),el("small",p?.title||"Unknown project"));if(meta)c.append(el("small",meta));c.append(el("p",r.content.slice(0,500)),button("Open record",()=>{projectId=r.projectId;moduleId=r.moduleId;openWorkspace()}));list.append(c)})}
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
 for(const [value,name] of [["all","All providers"],["crossref","Crossref"],["openalex","OpenAlex"],["pubmed","PubMed"]]){const opt=document.createElement("option");opt.value=value;opt.textContent=name;select.append(opt)}
 const emailBox=el("div","","feature"),emailLabel=el("label"),emailTitle=el("span","Optional contact email for provider polite access");const emailInput=document.createElement("input");emailInput.id="literatureEmail";emailInput.type="email";emailInput.maxLength=254;emailInput.autocomplete="email";emailInput.placeholder="you@example.org";emailInput.value=localStorage.getItem("amischolar.literature.email.v1")||"";emailLabel.append(emailTitle,emailInput);emailBox.append(emailLabel,el("p","Stored on this device. When you search, this email is sent to the selected public metadata providers and Unpaywall.","muted"),button("Save contact email",()=>{if(emailInput.value&&!emailInput.checkValidity()){notify("Enter a valid email address or leave the field blank.");return}localStorage.setItem("amischolar.literature.email.v1",emailInput.value.trim());notify("Contact email saved on this device.")}));
 const results=el("div","","list");results.id="literatureResults";row.append(select,button("Search research metadata",async()=>{
 results.replaceChildren(el("p","Searching Crossref, OpenAlex and PubMed…"));const q=input.value.trim(),email=emailInput.value.trim();
 if(email&&!emailInput.checkValidity()){results.replaceChildren(el("p","Enter a valid contact email or clear the optional field."));return}
 try{const result=await LiteratureAPI.search(q,select.value,20,email);results.replaceChildren();
 result.errors.forEach(error=>results.append(el("p",( {crossref:"Crossref",openalex:"OpenAlex",pubmed:"PubMed"}[error.provider]||error.provider)+" unavailable: "+error.message,"notice")));
 if(!result.records.length&&!result.errors.length)results.append(el("p","No matching metadata. Try different keywords."));
 result.records.forEach((item,index)=>{
  const card=el("article","","record");card.append(el("h3",item.title),el("p",[item.authors,item.year,item.journal].filter(Boolean).join(" · ")),el("small",item.doi?"DOI: "+item.doi:"No DOI in metadata"));
  if(item.abstract)card.append(el("p",item.abstract.slice(0,700)+(item.abstract.length>700?"…":"")));
  card.append(el("small","Open access: "+(item.oaStatus||"unknown")));
  if(item.oaUrl){try{const url=new URL(item.oaUrl);if(["https:","http:"].includes(url.protocol)){const link=el("a","Open full text");link.href=url.href;link.target="_blank";link.rel="noopener noreferrer";card.append(link)}}catch{}}
  const options=document.createElement("details"),summary=el("summary","Save options");options.append(summary);
  const statusLabel=el("label"),statusSpan=el("span","Reading status");const statusSelect=document.createElement("select");statusSelect.id="sourceStatus"+index;
  for(const [value,name] of [["to-read","To read"],["reading","Reading"],["cited","Cited"]]){const opt=document.createElement("option");opt.value=value;opt.textContent=name;statusSelect.append(opt)}
  statusLabel.append(statusSpan,statusSelect);const notesLabel=el("label"),notesSpan=el("span","Notes");const notes=document.createElement("textarea");notes.id="sourceNotes"+index;notes.placeholder="Add private notes about this source";notesLabel.append(notesSpan,notes);options.append(statusLabel,notesLabel);card.append(options);
  card.append(button("Import metadata",()=>{
   const entry=LiteratureAPI.record(item,statusSelect.value,notes.value.trim());const exists=data.records.some(r=>r.projectId===projectId&&r.moduleId==="literature-radar"&&item.doi&&r.content.includes("https://doi.org/"+item.doi));
   if(exists){notify("This DOI already exists in this paper.");return}
   data.records.push({id:uid(),projectId,moduleId:"literature-radar",title:entry.title,content:entry.content,updated:Date.now()});save();renderRecords();notify("Source imported as unverified metadata.");
  }));results.append(card)
 });
 if(!result.records.length&&result.errors.length)results.append(el("p","All selected providers failed. You can still add a source manually below."));
 }catch(e){results.replaceChildren(el("p","Search unavailable: "+e.message+". Check connectivity or provider CORS policy."))}},"secondary"));root.append(emailBox,row,results);
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
 for(const [value,label] of [["unsupported","Unsupported / not yet assessed"],["partially supported","Partially supported"],["supported","Supported (researcher assessed)"]]){const o=document.createElement("option");o.value=value;o.textContent=label;status.append(o)}
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
function renderTool(){if(typeof PDFReader!=="undefined")PDFReader.dispose();const panel=$("toolPanel"),actions=$("toolActions"),p=project(),rows=recordsFor();panel.replaceChildren();actions.replaceChildren();$("moduleHelp").classList.add("hidden");actions.append(button("❔ Help & research guidance",renderModuleHelp,"secondary"));$("liveSearch").classList.add("hidden");$("liveSearch").replaceChildren();const template=(title,content)=>actions.append(button("Use structured template",()=>addTemplate(title,content)));
switch(moduleId){
case "topic-lab":{toolLine(panel,"Research question: "+(p.question||"Not set — edit your paper to add one."));const q=(p.question||"").trim();analysisBox(panel,"Question quality checks",[!q?"Missing research question":null,q&&q.split(/\s+/).length<8?"Question may be too broad or short":null,q&&!/[?]$/.test(q)?"Consider phrasing the question explicitly":null,!/\b(how|what|why|whether|which|does|do|is|are|can|to what extent)\b/i.test(q)?"Specify a measurable outcome or explanatory relationship":null].filter(Boolean));actions.append(button("Edit research question",editPaper));template("Feasibility assessment","Population or system:\nResearch gap:\nVariables / outcomes:\nMethodology:\nData access:\nEthics considerations:\nTimeline and constraints:\nFeasibility decision:");break}
case "literature-radar":{toolLine(panel,"Search live scholarly metadata through Crossref, OpenAlex and PubMed when online. Queries are sent only when you search; imported records stay on this device and remain unverified until you check the original.");renderLiteratureSearch();template("Source title — author and year","Authors:\nYear:\nDOI or official URL:\nJournal / repository:\nSearch terms:\nDate accessed:\nAbstract summary (your own words):\nRelevance:\nVerified against original: No");analysisBox(panel,"Search planning",["Define keywords and synonyms","Record inclusion and exclusion criteria","Check publication date and study type","Verify every DOI against the original publication"]);break}
case "evidence-matrix":{template("Evidence from verified study","Claim:\nSupport status:\nSource citation / DOI:\nResearch design:\nSample / setting:\nMain findings (quote or paraphrase with page):\nLimitations and potential bias:\nRelevance to question:\nConfidence and justification:");const issues=[];rows.forEach(r=>{const f=parseFields(r.content);if(!f["source citation / doi"])issues.push(r.title+": missing source identifier");if(!f["limitations and potential bias"])issues.push(r.title+": limitations missing");if(!f["main findings (quote or paraphrase with page)"])issues.push(r.title+": finding missing")});rows.forEach(r=>{const f=parseFields(r.content);if(!["supported","partially supported","unsupported"].includes((f["support status (supported / partially supported / unsupported)"]||f["support status"]||"").toLowerCase()))issues.push(r.title+": explicitly assess support status");if(/unsupported/i.test(f["support status"]||f["support status (supported / partially supported / unsupported)"]||""))issues.push(r.title+": UNSUPPORTED CLAIM — revise or add valid evidence")});analysisBox(panel,"Evidence traceability",issues.length?issues:["No missing fields found; manually verify claims and sources."]);actions.append(button("Export evidence CSV",()=>exportEvidenceMatrix("csv")));actions.append(button("Export evidence Markdown",()=>exportEvidenceMatrix("markdown")));renderEvidenceLinks(panel,actions);break}
case "outline-architect":{template("Manuscript outline","Introduction:\nResearch gap:\nResearch question:\nMethods:\nResults / expected analysis:\nDiscussion:\nLimitations:\nConclusion:\nReferences and source links:");const issues=[];rows.forEach(r=>["introduction","method","conclusion","reference"].forEach(k=>{if(!r.content.toLowerCase().includes(k))issues.push(r.title+": consider adding "+k)}));analysisBox(panel,"Outline coverage",issues);break}
case "draft-studio":{const count=rows.reduce((n,r)=>n+r.content.trim().split(/\s+/).filter(Boolean).length,0);toolLine(panel,"Draft word count across saved records: "+count);template("Manuscript draft","Abstract:\nIntroduction:\nMethods:\nResults:\nDiscussion:\nLimitations:\nConclusion:\nReferences:\n");actions.append(button("Export draft text",()=>download("am-i-scholar-draft.txt",rows.map(r=>r.title+"\n"+r.content).join("\n\n"),"text/plain")));actions.append(button("Export Markdown",()=>exportManuscript("markdown")));actions.append(button("Export print-ready HTML",()=>exportManuscript("html")));actions.append(button("Print / Save PDF",()=>exportManuscript("print")));renderAiHandoff(panel);break}
case "style-coach":{template("Clarity revision","Original paragraph:\nRevised paragraph:\nWhy this improves clarity:\nTerminology / factual checks:");const drafts=data.records.filter(r=>r.projectId===projectId&&r.moduleId==="draft-studio");const long=drafts.flatMap(r=>r.content.split(/[.!?]+/).filter(s=>s.trim().split(/\s+/).length>30).map(s=>r.title+": "+s.trim().slice(0,110)));analysisBox(panel,"Offline writing signals",drafts.length?long.length?long:["No sentences over 30 words detected. This is not a full grammar check."]:["No draft found. Add text in Draft Studio first."]);break}
case "citation-center":{toolLine(panel,"Draft citation — verify against original source and target journal requirements. Local CSL styles do not validate your metadata.");template("Bibliographic record","Authors:\nYear:\nTitle:\nJournal / publisher:\nVolume / issue:\nPages:\nDOI or official URL:\nVerified against original: No");const issues=[];rows.forEach(r=>{const f=parseFields(r.content);["authors","year","doi or official url"].forEach(k=>{if(!f[k])issues.push(r.title+": missing "+k)})});analysisBox(panel,"Reference metadata audit",issues.length?issues:["No missing required fields in saved citation records. Verify metadata against the original before submitting."]);actions.append(button("Export CSL-JSON metadata",exportCslJson),button("Import CSL-JSON metadata",importCslJson),button("Export BibTeX",()=>exportBibliography("bibtex")),button("Import BibTeX",()=>importBibliography("bibtex")),button("Export RIS",()=>exportBibliography("ris")),button("Import RIS",()=>importBibliography("ris")));CitationWorkspace.render(panel,recordsFor(projectId,"citation-center").concat(recordsFor(projectId,"literature-radar")),parseFields,download,{records:()=>recordsFor(projectId,null),save:(title,content)=>{data.records.push({id:uid(),projectId,moduleId:"archive",title,content,updated:Date.now()});save();renderRecords();notify("PDF highlight saved locally with this paper.")}});break}
case "integrity":{template("Integrity and ethics review","Claims matched to sources:\nDirect quotations and page numbers:\nParaphrase attribution:\nHuman/animal ethics approval if applicable:\nConsent if applicable:\nConflicts of interest:\nFunding disclosure:\nAI-use disclosure per policy:\nData integrity and limitations:");analysisBox(panel,"Human review required",["No plagiarism database or similarity score is provided","Check citations and quotations against originals","Follow institution and journal disclosure policies","Verify ethical approvals before submission"]);break}
case "review-simulator":{template("Simulated reviewer response","Comment / potential concern:\nEvidence supporting response:\nResponse to reviewer:\nManuscript change:\nSection / line numbers:\nUnresolved issue:");analysisBox(panel,"Simulated reviewer checklist",["Is the research question novel and justified?","Can another researcher reproduce the method?","Do the findings support each conclusion?","Are limitations and biases clearly disclosed?","Are all sources verifiable?"]);break}
case "submission-kit":{template("Target journal requirements","Target journal:\nOfficial HTTPS author-guidelines URL:\nJournal scope and indexing:\nPublication fees verified:\nWord and figure limits:\nReporting checklist:\nEthics / consent:\nData availability statement:\nCover letter:\nSubmission readiness:");const issues=[];rows.forEach(r=>{const f=parseFields(r.content);if(!f["target journal"])issues.push(r.title+": journal missing");if(!/^https:\/\//i.test(f["official https author-guidelines url"]||""))issues.push(r.title+": official HTTPS guidelines link missing")});analysisBox(panel,"Journal due diligence",issues.length?issues:["Verify journal scope, editorial board, indexing and fees on official websites. No automated journal recommendation."]);break}
case "revision-response":{template("Revision record","Version:\nDate:\nReviewer comment:\nAuthor response:\nChanges made:\nManuscript excerpt:\nUnresolved items:");if(rows.length>1){const [latest,prior]=rows;const a=new Set(prior.content.split("\n").filter(Boolean)),b=new Set(latest.content.split("\n").filter(Boolean));analysisBox(panel,"Line-level changes: "+prior.title+" → "+latest.title,[...b].filter(x=>!a.has(x)).slice(0,6).map(x=>"+ "+x).concat([...a].filter(x=>!b.has(x)).slice(0,6).map(x=>"− "+x)))}else toolLine(panel,"Save two revision records to compare added and removed lines.");break}
case "archive":{template("Reproducibility record","Version and date:\nStudy protocol:\nData location and access permissions:\nSoftware and versions:\nParameters / environment:\nAnalysis steps:\nEthics and privacy considerations:\nLimitations:\nReproduction checklist:");toolLine(panel,"This project contains "+recordsFor(projectId,null).length+" local records. Export a portable backup in Settings.");actions.append(button("Export this paper for Android",()=>exportAndroid(projectId)));break}
}}
function renderAiHandoff(panel){
 const box=el("section","","panel");box.id="aiHandoff";
 box.append(el("h3","Manual AI handoff"));
 box.append(el("p","This app does not make AI requests. Review this exact prompt, then choose whether to copy it into an external service. External services may charge fees or retain data under their own terms."));
 box.append(el("p","Only source records you select and linked evidence notes are included. Drafts, project questions, PDFs and unrelated notes are excluded."));
 const task=document.createElement("textarea");task.id="aiTask";task.maxLength=AiHandoff.MAX_TASK_CHARS;task.value="Summarize the selected evidence and identify limitations.";
 const taskLabel=el("label","Task to request");taskLabel.htmlFor=task.id;box.append(taskLabel,task);
 const sources=recordsFor(projectId,"literature-radar").concat(recordsFor(projectId,"citation-center")).map(r=>{
  const fields=parseFields(r.content),identifier=fields["doi or official url"]||fields["doi or url"]||"";
  const linked=(data.evidenceLinks||[]).filter(l=>l.projectId===projectId&&l.sourceId===r.id).map(l=>{
   const note=recordsFor(projectId,"evidence-matrix").find(n=>n.id===l.claimId);
   return note?note.title+": "+note.content+"\nResearcher assessment: "+l.status+". "+(l.rationale||""):"";
  }).filter(Boolean);
  return {id:r.id,title:r.title,identifier,verified:/^yes$/i.test(fields["verified against original"]||""),evidence:[fields["abstract summary (your own words)"]||fields["evidence notes"]||"",...linked].filter(Boolean).join("\n")};
 });
 const selected=new Set(),sourceBox=el("div","","list");
 const consent=document.createElement("input");consent.type="checkbox";consent.id="aiShareConsent";
 const consentLabel=el("label","I reviewed this exact prompt and consent to copying the selected notes for possible external processing. This consent applies once.");consentLabel.htmlFor=consent.id;
 const preview=el("pre","","record");preview.id="aiPromptPreview";preview.style.whiteSpace="pre-wrap";preview.style.maxHeight="320px";preview.style.overflow="auto";
 const limit=el("p");limit.id="aiPromptLimit";
 const status=el("p");status.id="aiHandoffStatus";status.setAttribute("role","status");
 const saveButton=button("Save AI-use log locally",()=>{const built=promptForSelection();if(!AiHandoff.canCopy(true,built)||!response.value.trim())return;const disclosureText=disclosure.value||AiHandoff.disclosure({provider:provider.value,task:task.value,sourceIds:built.sourceIds,reviewed:reviewed.checked});data.records.push({id:uid(),projectId,moduleId:"archive",title:"AI-use log · "+new Date().toISOString(),content:"AI-assisted text — unverified by the app\nExternal service: "+(provider.value.trim()||"not recorded")+"\nTask: "+task.value+"\nSelected source IDs: "+built.sourceIds.join(", ")+"\nResearcher review checkbox: "+(reviewed.checked?"checked":"not checked")+"\n\nExact prompt:\n"+built.prompt+"\n\nReturned text:\n"+response.value+"\n\n"+disclosureText,updated:Date.now()});save();saveButton.disabled=true;notify("AI-use log saved locally under Archive.");renderRecords()});saveButton.id="aiSaveLog";saveButton.disabled=true;
 const provider=document.createElement("input");provider.id="aiProvider";provider.maxLength=120;provider.placeholder="External AI service name (optional)";provider.disabled=true;
 const response=document.createElement("textarea");response.id="aiResponse";response.maxLength=20000;response.placeholder="Paste the returned text here";response.disabled=true;
 const reviewed=document.createElement("input");reviewed.type="checkbox";reviewed.id="aiReviewed";reviewed.disabled=true;const disclosure=document.createElement("textarea");disclosure.id="aiDisclosure";disclosure.setAttribute("aria-label","Editable AI-use disclosure statement");let disclosureEdited=false;function updateDisclosure(){if(!disclosureEdited)disclosure.value=AiHandoff.disclosure({provider:provider.value,task:task.value,sourceIds:promptForSelection().sourceIds,reviewed:reviewed.checked})}disclosure.addEventListener("input",()=>disclosureEdited=true);
 const reviewedLabel=el("label","I checked the returned claims and citations against the original sources.");reviewedLabel.htmlFor=reviewed.id;
 const disclosureButton=button("Download disclosure statement",()=>download("ai-use-disclosure.txt",disclosure.value||AiHandoff.disclosure({provider:provider.value,task:task.value,sourceIds:promptForSelection().sourceIds,reviewed:reviewed.checked}),"text/plain"));disclosureButton.id="aiExportDisclosure";
 function promptForSelection(){return AiHandoff.buildPrompt(task.value,[...selected].map(id=>sources.find(x=>x.id===id)).filter(Boolean))}
 const copyButton=button("Copy reviewed prompt",async()=>{const built=promptForSelection();if(!AiHandoff.canCopy(consent.checked,built))return;try{await copyHandoffText(built.prompt);consent.checked=false;saveButton.disabled=false;status.textContent="Prompt copied. Choose an external provider yourself; this app has not sent it."}catch(e){status.textContent="Clipboard unavailable. Select and copy the reviewed preview manually."}});copyButton.id="aiCopyPrompt";copyButton.disabled=true;
 function refresh(){const result=promptForSelection();preview.textContent=result.prompt;updateDisclosure();limit.textContent="Selected source records: "+result.sourceIds.length+" · Prompt: "+result.prompt.length+"/"+AiHandoff.MAX_PROMPT_CHARS+" characters"+(result.truncated?" · Context shortened; review the full preview.":"");copyButton.disabled=!AiHandoff.canCopy(consent.checked,result)}
 sources.forEach(source=>{const label=el("label","","record");const check=document.createElement("input");check.type="checkbox";check.id="ai-source-"+source.id;check.onchange=()=>{if(check.checked)selected.add(source.id);else selected.delete(source.id);consent.checked=false;saveButton.disabled=true;refresh()};label.append(check,document.createTextNode(" "+source.title+" · "+(source.identifier||"No DOI/URL recorded")+" · "+(source.verified?"marked verified":"UNVERIFIED")));sourceBox.append(label)});
 const external=el("p","Selected sources may contain research notes. Do not copy private information unless you intentionally consent to share it.");
 task.addEventListener("input",()=>{consent.checked=false;saveButton.disabled=true;refresh()});
 consent.addEventListener("change",refresh);
 copyButton.addEventListener("click",()=>{provider.disabled=false;response.disabled=false;reviewed.disabled=false});
 box.append(sourceBox,external,el("h4","Prompt preview"),preview,limit,consent,consentLabel,copyButton,status,provider,response,reviewed,reviewedLabel,el("label","Editable AI-use disclosure statement"),disclosure,saveButton,disclosureButton);
 panel.append(box);refresh();
}
async function copyHandoffText(text){
 if(navigator.clipboard?.writeText){try{return await navigator.clipboard.writeText(text)}catch(_){}}
 const area=document.createElement("textarea");area.value=text;area.style.position="fixed";area.style.left="-9999px";document.body.append(area);area.select();const copied=document.execCommand("copy");area.remove();if(!copied)throw Error("Clipboard copy failed");
}
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
function importCslJson(){
 const input=document.createElement("input");input.type="file";input.accept=".json,application/json";
 input.onchange=async()=>{const file=input.files?.[0];if(!file)return;
  try{
   if(file.size>5*1024*1024)throw Error("CSL-JSON file exceeds 5 MB.");
   const obj=JSON.parse(await file.text());if(!Array.isArray(obj)||obj.length>2000)throw Error("Expected an array of up to 2,000 CSL-JSON items.");
   const imported=obj.map(x=>{
    if(!x||typeof x!=="object"||typeof x.title!=="string"||!x.title.trim())throw Error("Each CSL item needs a title.");
    const authors=Array.isArray(x.author)?x.author.map(a=>a.literal||[a.family,a.given].filter(Boolean).join(", ")).filter(Boolean).join("; "):"";
    const year=x.issued?.["date-parts"]?.[0]?.[0]||"";
    const doi=typeof x.DOI==="string"&&/^10\.\d{4,9}\//i.test(x.DOI)?x.DOI:"";
    const url=typeof x.URL==="string"&&/^https?:\/\//i.test(x.URL)?x.URL:"";
    const content=["CSL-JSON: "+safeText(JSON.stringify(x),50000),"Authors: "+safeText(authors,3000),"Year: "+safeText(String(year),10),"Title: "+safeText(x.title,1000),"Journal / publisher: "+safeText(x["container-title"],1000),"DOI or official URL: "+safeText(doi||url,2000),"Provenance: User-imported CSL-JSON metadata","Verified against original: No"].join("\n");
    return {id:uid(),projectId,moduleId:"citation-center",title:safeText(x.title,1000),content,updated:Date.now()};
   });
   if(!confirm("Import "+imported.length+" unverified CSL-JSON source records into this paper?"))return;
   data.records.push(...imported);save();renderRecords();renderTool();notify("Imported CSL-JSON metadata; all entries remain unverified.");
  }catch(e){notify("CSL-JSON import failed: "+e.message)}
 };
 input.click();
}
function exportCslJson(){
 const p=project();if(!p)return;
 const rows=recordsFor(projectId,"citation-center").concat(recordsFor(projectId,"literature-radar"));
 const entries=rows.map(r=>{
  const f=parseFields(r.content);if(f["csl-json"]){try{const original=JSON.parse(f["csl-json"]);if(original&&typeof original==="object"&&original.title){return {...original,id:r.id}}}catch{}}const authors=(f.authors||"").split(";").map(n=>n.trim()).filter(Boolean).map(name=>({literal:name}));
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
function bibliographyRecords(){return recordsFor(projectId,"citation-center").concat(recordsFor(projectId,"literature-radar"))}
function bibliographyItems(){return bibliographyRecords().map(r=>CitationWorkspace.cslItem(r,parseFields))}
function exportBibliography(format){const items=bibliographyItems();if(!items.length){notify("Add source records before exporting a bibliography.");return}const bib=format==="bibtex",content=bib?BibliographyFormats.toBibTeX(items):BibliographyFormats.toRIS(items);download(bib?"am-i-scholar-bibliography.bib":"am-i-scholar-bibliography.ris",content,bib?"application/x-bibtex":"application/x-research-info-systems");notify((bib?"BibTeX":"RIS")+" bibliography exported.")}
function importBibliography(format){const bib=format==="bibtex",input=document.createElement("input");input.type="file";input.accept=bib?".bib,text/plain,application/x-bibtex":".ris,text/plain,application/x-research-info-systems";input.onchange=async()=>{const file=input.files?.[0];if(!file)return;try{if(file.size>5*1024*1024)throw Error("Bibliography file exceeds 5 MB.");const source=await file.text(),items=bib?BibliographyFormats.parseBibTeX(source):BibliographyFormats.parseRIS(source);if(items.length>2000)throw Error("Import is limited to 2,000 records.");const imported=items.map(item=>{const authors=(item.author||[]).map(a=>a.literal||[a.family,a.given].filter(Boolean).join(", ")).filter(Boolean).join("; "),year=item.issued?.["date-parts"]?.[0]?.[0]||"",doi=item.DOI||"",url=item.URL||"",volume=[item.volume,item.issue?"("+item.issue+")":""].filter(Boolean).join(""),content=["CSL-JSON: "+safeText(JSON.stringify(item),50000),"Authors: "+safeText(authors,3000),"Year: "+safeText(String(year),10),"Title: "+safeText(item.title,1000),"Journal / publisher: "+safeText(item["container-title"]||item.publisher,1000),"Volume / issue: "+safeText(volume,100),"Pages: "+safeText(item.page,100),"DOI or official URL: "+safeText(doi||url,2000),"Abstract: "+safeText(item.abstract,10000),"Provenance: User-imported "+(bib?"BibTeX":"RIS")+" metadata","Verified against original: No"].join("\n");return {id:uid(),projectId,moduleId:"citation-center",title:safeText(item.title,1000),content,updated:Date.now()}});if(!confirm("Import "+imported.length+" unverified "+(bib?"BibTeX":"RIS")+" records into this paper?"))return;data.records.push(...imported);save();renderRecords();renderTool();notify("Imported "+imported.length+" unverified "+(bib?"BibTeX":"RIS")+" records.")}catch(e){notify((bib?"BibTeX":"RIS")+" import failed: "+e.message)}};input.click()}
function download(name,content,mime="application/json"){const blob=new Blob([content],{type:mime}),url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
function webBackup(){download("am-i-scholar-web-backup.json",JSON.stringify({format:"am-i-scholar-web",version:2,projects:data.projects,records:data.records,evidenceLinks:data.evidenceLinks},null,2));notify("Backup exported. Store it somewhere safe.")}
async function encryptedWebBackup(){
 if(!window.AmIScholarBackupCrypto)throw Error("Encryption module unavailable.");
 const pass=prompt("Create a passphrase (at least 12 characters). Keep it separately; it cannot be recovered.");if(pass===null)return;
 const again=prompt("Enter the passphrase again to confirm.");if(again===null)return;if(pass!==again)throw Error("Passphrases do not match.");
 const payload={format:"am-i-scholar-web",version:2,projects:data.projects,records:data.records,evidenceLinks:data.evidenceLinks};
 const encrypted=await AmIScholarBackupCrypto.encrypt(payload,pass);
 download("am-i-scholar-encrypted-backup.json",encrypted,"application/json");notify("Encrypted research backup exported. Store the file and passphrase separately.");
}
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
function importBackup(obj,confirmed=false){if(!obj||typeof obj!=="object")throw Error("Invalid JSON");if(obj.format==="am-i-scholar-web"&&(obj.version===1||obj.version===2)){if(!Array.isArray(obj.projects)||!Array.isArray(obj.records)||obj.projects.length+obj.records.length+(Array.isArray(obj.evidenceLinks)?obj.evidenceLinks.length:0)>10000)throw Error("Invalid web backup");for(const p of obj.projects)if(typeof p.id!=="string"||typeof p.title!=="string")throw Error("Invalid project");for(const r of obj.records)if(typeof r.id!=="string"||typeof r.projectId!=="string"||typeof r.moduleId!=="string"||typeof r.title!=="string"||typeof r.content!=="string")throw Error("Invalid record");if(!confirmed&&!confirm("Replace ALL current browser research with this web backup? Export your current data first."))return;data={version:2,projects:obj.projects,records:obj.records,evidenceLinks:Array.isArray(obj.evidenceLinks)?obj.evidenceLinks.filter(l=>l&&typeof l.id==="string"&&typeof l.claimId==="string"&&typeof l.sourceId==="string"&&["supported","partially supported","unsupported"].includes(l.status)):[]};save();renderHome();notify("Web backup restored.");return}
if(obj.format==="am-i-scholar-project"&&obj.version===1){const p=obj.project;if(!p||typeof p.title!=="string"||!Array.isArray(obj.notes)||!Array.isArray(obj.sources)||obj.notes.length+obj.sources.length+(Array.isArray(obj.evidenceLinks)?obj.evidenceLinks.length:0)>10000)throw Error("Invalid Android project backup");if(!confirmed&&!confirm("Import this Android research project as a NEW browser paper?"))return;const id=uid(),next={id,title:safeText(p.title,200),question:safeText(p.researchQuestion),discipline:safeText(p.discipline),citationStyle:safeText(p.citationStyle)||"APA"};if(!next.title.trim())throw Error("Empty project title");const imported=[],noteIds=[],sourceIds=[];const kinds={TOPIC:"topic-lab",EVIDENCE:"evidence-matrix",OUTLINE:"outline-architect",DRAFT:"draft-studio",STYLE:"style-coach",INTEGRITY:"integrity",REVIEW:"review-simulator",SUBMISSION:"submission-kit",REVISION:"revision-response",ARCHIVE:"archive"};for(const n of obj.notes){if(typeof n.title!=="string"||typeof n.content!=="string")throw Error("Invalid note");const record={id:uid(),projectId:id,moduleId:kinds[n.kind]||"draft-studio",title:safeText(n.title),content:safeText(n.content),updated:Date.now()};noteIds.push(record.id);imported.push(record)}for(const s of obj.sources){if(typeof s.title!=="string")throw Error("Invalid source");const record={id:uid(),projectId:id,moduleId:"literature-radar",title:safeText(s.title),content:"Authors: "+safeText(s.authors)+"\nYear: "+safeText(s.year)+"\nDOI or official URL: "+safeText(s.doiOrUrl)+"\nJournal / publisher: "+safeText(s.publication)+"\nVerified against original: "+(s.verified?"Yes":"No")+"\nEvidence notes: "+safeText(s.evidenceNotes),updated:Date.now()};sourceIds.push(record.id);imported.push(record)}const links=[];for(const l of obj.evidenceLinks||[]){if(!Number.isInteger(l.noteIndex)||!Number.isInteger(l.sourceIndex)||!noteIds[l.noteIndex]||!sourceIds[l.sourceIndex]||obj.notes[l.noteIndex]?.kind!=="EVIDENCE"||!["supported","partially supported","unsupported"].includes(l.status)||typeof l.rationale!=="string")throw Error("Invalid evidence relationship in backup");links.push({id:uid(),projectId:id,claimId:noteIds[l.noteIndex],sourceId:sourceIds[l.sourceIndex],status:l.status,rationale:safeText(l.rationale),createdAt:new Date().toISOString()})}data.evidenceLinks=data.evidenceLinks||[];data.evidenceLinks.push(...links);data.projects.push(next);data.records.push(...imported);save();projectId=id;renderModules();notify("Android project imported as a new browser paper.");return}
throw Error("Unsupported backup format or version")}
$("projectForm").onsubmit=e=>{e.preventDefault();const title=$("projectTitle").value.trim();if(!title)return;let p=project();if(!p){p={id:uid()};data.projects.push(p)}p.title=title;p.question=$("projectQuestion").value;p.discipline=$("projectDiscipline").value;p.citationStyle=$("projectCitation").value;projectId=p.id;save();renderModules();notify("Research paper saved.")};
$("recordForm").onsubmit=e=>{e.preventDefault();const title=$("recordTitle").value.trim(),content=$("recordContent").value.trim();if(!title||!content)return;let r=data.records.find(r=>r.id===editId);if(!r){r={id:uid(),projectId,moduleId};data.records.push(r)}r.title=title;r.content=content;r.updated=Date.now();save();editId=null;$("recordForm").reset();renderRecords();renderTool();notify("Record saved on this device.")};
$("clearRecord").onclick=()=>{editId=null;$("recordForm").reset()};
$("headerNew").onclick=$("heroNew").onclick=$("projectNew").onclick=newPaper;
$("cancelProject").onclick=()=>project()?renderModules():renderProjects();
$("backWorkspace").onclick=renderModules;$("editPaper").onclick=editPaper;$("switchPaper").onclick=renderProjects;
$("backupWeb").onclick=webBackup;$("backupEncryptedWeb").onclick=()=>encryptedWebBackup().catch(e=>notify("Encrypted export failed: "+e.message));$("backupAndroid").onclick=()=>choosePaper(()=>exportAndroid(projectId));
$("restoreFile").onchange=async e=>{const file=e.target.files?.[0];if(!file)return;try{if(file.size>10*1024*1024)throw Error("File exceeds 10 MB");const text=await file.text(),parsed=JSON.parse(text);if(parsed?.format==="am-i-scholar-encrypted"){const pass=prompt("Enter the backup passphrase.");if(pass===null)return;const payload=await AmIScholarBackupCrypto.decrypt(text,pass);if(payload?.format!=="am-i-scholar-web"||![1,2].includes(payload.version)||!Array.isArray(payload.projects)||!Array.isArray(payload.records))throw Error("Encrypted file does not contain a supported research backup.");const total=payload.projects.length+payload.records.length+(Array.isArray(payload.evidenceLinks)?payload.evidenceLinks.length:0);if(total>10000)throw Error("Too many records.");if(!confirm("Restore encrypted research backup? This replaces "+payload.projects.length+" papers and "+payload.records.length+" records on this browser. Save current data first."))return;importBackup(payload,true)}else importBackup(parsed)}catch(err){notify("Restore failed: "+err.message)}finally{e.target.value=""}};
function connection(){const online=navigator.onLine;$("connection").textContent=online?"● Online":"● Offline";$("connection").style.background=online?"#e3f6ed":"#fff0d9"}
window.addEventListener("online",connection);window.addEventListener("offline",connection);connection();
if("serviceWorker" in navigator)window.addEventListener("load",()=>navigator.serviceWorker.register("./sw.js").catch(()=>notify("Offline installation unavailable in this browser.")));
renderHome();
