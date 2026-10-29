const {test,expect}=require("@playwright/test");
const fs=require("node:fs/promises");
const {parseICS,serializeICS,formatWhen}=require("../calendar-ics.js");

test("ICS import parses timezones, DST transitions, folded text, escapes, and all-day dates",async()=>{
 const source=await fs.readFile("tests/fixtures/student-calendar-timezones.ics","utf8");
 const {events}=parseICS(source);
 expect(events).toHaveLength(4);
 const dst=events.find(event=>event.uid==="dst-transition@example.test");
 expect(new Date(dst.startAt).toISOString()).toBe("2026-03-08T06:30:00.000Z");
 expect(new Date(dst.endAt).toISOString()).toBe("2026-03-08T07:30:00.000Z");
 expect(dst.timeZone).toBe("America/New_York");
 expect(dst.description).toBe("Bring slides, questions; and notes");
 expect(formatWhen(dst,"America/Los_Angeles")).toContain("Mar 7, 2026, 10:30 PM");
 expect(formatWhen(dst,"Asia/Dubai")).toContain("Mar 8, 2026, 10:30 AM");
 const allDay=events.find(event=>event.uid==="all-day-study@example.test");
 expect(allDay).toMatchObject({allDay:true,startDate:"2026-03-09",endDateExclusive:"2026-03-10"});
 const utc=events.find(event=>event.uid==="utc-workshop@example.test");
 expect(utc.startAt).toBe(Date.parse("2026-10-28T12:00:00Z"));
 const folded=events.find(event=>event.uid==="folded-summary@example.test");
 expect(folded.title).toBe("Folded conference session that continues with a second line");
 const serialized=parseICS(serializeICS(events));
 expect(serialized.events).toHaveLength(4);
 expect(serialized.events.find(event=>event.uid===dst.uid).startAt).toBe(dst.startAt);
 expect(serialized.events.find(event=>event.uid===allDay.uid).endDateExclusive).toBe("2026-03-10");
});

async function createPaper(page,title="Characterization paper"){
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill(title);
 await page.locator("#projectQuestion").fill("How does the measurable outcome vary across the study population?");
 await page.locator("#projectDiscipline").fill("Biology");
 await page.getByRole("button",{name:"Save paper"}).click();
}

test("paper form preserves six citation choices; edit, switch, and record clear remain reachable",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await expect(page.locator("#projectCitation option")).toHaveText(["APA","MLA","Chicago","IEEE","Harvard","Vancouver"]);
 await page.locator("#projectTitle").fill("Paper One");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Topic Lab"}).click();
 await page.getByRole("button",{name:"Use structured template"}).click();
 await page.locator("#recordTitle").fill("Feasibility");
 await page.locator("#recordContent").fill("First draft");
 await page.getByRole("button",{name:"Save record"}).click();
 await page.getByRole("button",{name:"Edit",exact:true}).click();
 await page.locator("#recordContent").fill("Revised draft");
 await page.getByRole("button",{name:"Save record"}).click();
 await expect(page.locator("#recordList")).toContainText("Revised draft");
 await page.locator("#recordTitle").fill("Unsaved");
 await page.locator("#recordContent").fill("Unsaved content");
 await page.getByRole("button",{name:"Clear",exact:true}).click();
 await expect(page.locator("#recordTitle")).toHaveValue("");
 page.once("dialog",dialog=>dialog.accept());
 await page.getByRole("button",{name:"Delete",exact:true}).click();
 await expect(page.locator("#recordList")).toContainText("No records yet");
 await page.getByRole("button",{name:"Edit paper"}).click();
 await page.locator("#projectTitle").fill("Paper One edited");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await expect(page.locator("#projectList")).toContainText("Paper One edited");
 await page.getByRole("button",{name:"+ Create paper"}).click();
 await page.locator("#projectTitle").fill("Paper Two");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await page.locator("#projectList").getByRole("button",{name:"Open paper"}).first().click();
 await page.locator("#allModules").getByRole("button",{name:"Open Topic Lab"}).click();
 await expect(page.locator("#workspacePaper")).toHaveText("Paper Two");
});

test("connection indicator follows browser online and offline events",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.evaluate(()=>{Object.defineProperty(navigator,"onLine",{configurable:true,value:false});window.dispatchEvent(new Event("offline"))});
 await expect(page.locator("#connection")).toContainText("Offline");
 await page.evaluate(()=>{Object.defineProperty(navigator,"onLine",{configurable:true,value:true});window.dispatchEvent(new Event("online"))});
 await expect(page.locator("#connection")).toContainText("Online");
});

test("each research module opens its distinct baseline workflow and structured template",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await createPaper(page,"Module inventory");
 const cases=[
  ["Topic Lab","Question quality checks","Population or system"],
  ["Literature Radar","Search live scholarly metadata","Authors:"],
  ["Evidence Matrix","Evidence traceability","Support status:"],
  ["Outline Architect","Outline coverage","Research gap:"],
  ["Draft Studio","Draft word count","Abstract:"],
  ["Style Coach","Offline writing signals","Original paragraph:"],
  ["Citation Center","Offline CSL bibliography","DOI or official URL:"],
  ["Originality & Integrity","Human review required","Direct quotations and page numbers:"],
  ["Review Simulator","Simulated reviewer checklist","Comment / potential concern:"],
  ["Submission Kit","Journal due diligence","Official HTTPS author-guidelines URL:"],
  ["Revision & Response","Save two revision records","Reviewer comment:"],
  ["Archive & Reproducibility","portable backup","Reproduction checklist:"]
 ];
 for(const [name,toolText,templateText] of cases){
  await page.locator("#allModules").getByRole("button",{name:"Open "+name}).click();
  await expect(page.locator("#toolPanel")).toContainText(toolText);
  await page.getByRole("button",{name:"Use structured template"}).click();
  expect(await page.locator("#recordContent").inputValue()).toContain(templateText);
  await page.getByRole("button",{name:"← Modules"}).click();
 }
});

test("source library opens saved source records; legacy web and Android backups restore",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await createPaper(page,"Source owner");
 await page.locator("#allModules").getByRole("button",{name:"Open Literature Radar"}).click();
 await page.locator("#recordTitle").fill("Local source");
 await page.locator("#recordContent").fill("Authors: A. Researcher\nYear: 2024\nDOI or official URL: https://doi.org/10.1000/example");
 await page.getByRole("button",{name:"Save record"}).click();
 await page.locator("aside").getByRole("button",{name:/Source library/}).click();
 await expect(page.locator("#sourceList")).toContainText("Local source");
 await page.getByRole("button",{name:"Open record"}).click();
 await expect(page.locator("#workspaceTitle")).toHaveText("Literature Radar");

 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const restoreDialog=page.waitForEvent("dialog");
 await page.locator("#restoreFile").setInputFiles({name:"old-web.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify({format:"am-i-scholar-web",version:1,projects:[{id:"old-project",title:"Version one"}],records:[]}))});
 await (await restoreDialog).accept();
 await expect(page.locator("#status")).toHaveText("Web backup restored.");
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await expect(page.locator("#projectList")).toContainText("Version one");
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const android={format:"am-i-scholar-project",version:1,project:{title:"Android import",researchQuestion:"A question?",discipline:"Biology",citationStyle:"APA"},notes:[{title:"Method",content:"Local note",kind:"DRAFT"}],sources:[{title:"Source",authors:"A. Author",year:"2023",doiOrUrl:"10.1000/doi",publication:"Journal",verified:false}],evidenceLinks:[]};

 await page.locator("#restoreFile").setInputFiles({name:"android.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify(android))});
 await expect(page.locator("#status")).toHaveText("Android project imported as a new browser paper.");
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await expect(page.locator("#projectList")).toContainText("Android import");
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const webV2={format:"am-i-scholar-web",version:2,projects:[{id:"v2-project",title:"Version two",question:"Stored question?",discipline:"Biology",citationStyle:"APA"}],records:[{id:"v2-record",projectId:"v2-project",moduleId:"draft-studio",title:"Restored draft",content:"Preserved text",updated:1}],evidenceLinks:[]};

 await page.locator("#restoreFile").setInputFiles({name:"current-web.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify(webV2))});
 await expect(page.locator("#status")).toHaveText("Web backup restored.");
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await expect(page.locator("#projectList")).toContainText("Version two");
});

test("student focus sessions appear in the unified calendar with dated tasks",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Focus"}).click();
 await page.locator('#academicBody input[type="number"]').fill("1");
 await page.getByRole("button",{name:"Start",exact:true}).click();
 await page.getByRole("button",{name:"Pause / resume"}).click();
 await page.getByRole("button",{name:"Pause / resume"}).click();
 await page.getByRole("button",{name:"Finish & log"}).click();
 await expect(page.locator("#academicBody")).toContainText("Logged focus time");
 await page.locator("#academicNav").getByRole("button",{name:"Tasks"}).click();
 await page.getByRole("button",{name:"+ New task"}).click();
 await page.locator('#academicBody input[name="title"]').fill("Calendar characterization");
 await page.locator('#academicBody input[name="dueDate"]').fill(new Date().toISOString().slice(0,10));
 await page.getByRole("button",{name:"Save",exact:true}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Calendar"}).click();
 await expect(page.locator("#academicBody")).toContainText("Calendar characterization");
 await expect(page.locator("#academicBody")).toContainText("focus");
});

test("OpenAlex search imports its returned metadata as an unverified source",async({page})=>{
 await page.route("https://api.openalex.org/works?*",route=>route.fulfill({status:200,contentType:"application/json",body:JSON.stringify({results:[{display_name:"OpenAlex characterization",authorships:[{author:{display_name:"A. Scholar"}}],publication_year:2023,doi:"https://doi.org/10.1000/openalex",primary_location:{source:{display_name:"Example Venue"}}}]})}));
 await page.goto("http://127.0.0.1:8000/");
 await createPaper(page,"OpenAlex source test");
 await page.locator("#allModules").getByRole("button",{name:"Open Literature Radar"}).click();
 await page.locator("#literatureQuery").fill("research metadata");
 await page.locator("#literatureProvider").selectOption("openalex");
 await page.getByRole("button",{name:"Search research metadata"}).click();
 await expect(page.locator("#literatureResults")).toContainText("OpenAlex characterization");
 await expect(page.locator("#literatureResults")).toContainText("A. Scholar");
 await page.getByRole("button",{name:"Import metadata"}).click();
 await expect(page.locator("#recordList")).toContainText("Verified against original: No");
});

test("offline literature search reports an error and leaves manual source entry available",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await createPaper(page,"Offline search test");
 await page.locator("#allModules").getByRole("button",{name:"Open Literature Radar"}).click();
 await page.locator("#literatureQuery").fill("offline research query");
 await page.evaluate(()=>{Object.defineProperty(navigator,"onLine",{configurable:true,value:false})});
 await page.getByRole("button",{name:"Search research metadata"}).click();
 await expect(page.locator("#literatureResults")).toContainText("Offline: manual source entry remains available.");
 await expect(page.locator("#recordTitle")).toBeVisible();
 await expect(page.locator("#recordContent")).toBeVisible();
});

test("federated literature search deduplicates 20 sources and saves Open Access metadata, status, and notes",async({page})=>{
 const crossref=Array.from({length:10},(_,n)=>({title:["Crossref title "+(n+1)],author:[{given:"Ada",family:"Author"}],DOI:"10.1000/paper-"+(n+1),published:{"date-parts":[[2024]]},"container-title":["Journal A"],abstract:"<jats:p>Crossref abstract</jats:p>"}));
 const openalex=Array.from({length:10},(_,n)=>({display_name:"OpenAlex title "+(n+6),authorships:[{author:{display_name:"Bea Researcher"}}],publication_year:2023,doi:"https://doi.org/10.1000/paper-"+(n+6),primary_location:{source:{display_name:"Journal B"}},abstract_inverted_index:{Open:[0],Alex:[1],abstract:[2]},open_access:{is_oa:true,oa_status:"gold",best_oa_location:{landing_page_url:"https://oa.example/article"}}}));
 const pmids=Array.from({length:10},(_,n)=>String(n+1));
 const articles=pmids.map((id,n)=>`<PubmedArticle><MedlineCitation><PMID>${id}</PMID><Article><ArticleTitle>PubMed title ${n+11}</ArticleTitle><AuthorList><Author><ForeName>Cy</ForeName><LastName>Scholar</LastName></Author></AuthorList><Journal><Title>Journal C</Title><JournalIssue><PubDate><Year>2022</Year></PubDate></JournalIssue></Journal><Abstract><AbstractText Label="RESULTS">PubMed abstract</AbstractText></Abstract><ELocationID EIdType="doi">10.1000/paper-${n+11}</ELocationID></Article></MedlineCitation></PubmedArticle>`).join("");
 const requestUrls=[];
 await page.route("https://api.crossref.org/works?*",route=>{requestUrls.push(route.request().url());return route.fulfill({status:200,contentType:"application/json",body:JSON.stringify({message:{items:crossref}})})});
 await page.route("https://api.openalex.org/works?*",route=>{requestUrls.push(route.request().url());return route.fulfill({status:200,contentType:"application/json",body:JSON.stringify({results:openalex})})});
 await page.route("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?*",route=>{requestUrls.push(route.request().url());return route.fulfill({status:200,contentType:"application/json",body:JSON.stringify({esearchresult:{idlist:pmids}})})});
 await page.route("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?*",route=>{requestUrls.push(route.request().url());return route.fulfill({status:200,contentType:"application/xml",body:`<?xml version="1.0"?><PubmedArticleSet>${articles}</PubmedArticleSet>`})});
 await page.route("https://api.unpaywall.org/v2/**",route=>{requestUrls.push(route.request().url());return route.fulfill({status:200,contentType:"application/json",body:JSON.stringify({is_oa:true,oa_status:"gold",best_oa_location:{url_for_pdf:"https://oa.example/fulltext.pdf"}})})});
 await page.goto("http://127.0.0.1:8000/");
 await createPaper(page,"Federated search");
 await page.locator("#allModules").getByRole("button",{name:"Open Literature Radar"}).click();
 await page.locator("#literatureQuery").fill("CRISPR off-target effects");
 await page.locator("#literatureEmail").fill("researcher@example.org");
 await page.getByRole("button",{name:"Save contact email"}).click();
 await page.getByRole("button",{name:"Search research metadata"}).click();
 await expect(page.locator("#literatureResults article.record")).toHaveCount(20,{timeout:15000});
 const first=page.locator("#literatureResults article.record").nth(0);
 await expect(first).toContainText("Crossref abstract");
 await expect(first).toContainText("Open access: gold");
 await expect(first.getByRole("link",{name:"Open full text"})).toHaveAttribute("href","https://oa.example/fulltext.pdf");
 await first.locator("details summary").click();
 await first.locator("details select").selectOption("reading");
 await first.locator("details textarea").fill("Check sample size and methods.");
 await first.getByRole("button",{name:"Import metadata"}).click();
 await expect(page.locator("#recordList")).toContainText("Status: reading");
 await expect(page.locator("#recordList")).toContainText("Notes: Check sample size and methods.");
 expect(requestUrls.some(url=>new URL(url).searchParams.get("mailto")==="researcher@example.org")).toBeTruthy();
 expect(requestUrls.some(url=>new URL(url).searchParams.get("email")==="researcher@example.org")).toBeTruthy();
 expect(requestUrls.some(url=>url.includes("esearch.fcgi"))).toBeTruthy();
});

test("web, Android, student, evidence, and CSL export entry points remain available",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await createPaper(page,"Export owner");
 await page.locator("#allModules").getByRole("button",{name:"Open Draft Studio"}).click();
 await page.locator("#recordTitle").fill("Draft");
 await page.locator("#recordContent").fill("A draft paragraph.");
 await page.getByRole("button",{name:"Save record"}).click();
 const [draft]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export draft text"}).click()]);
 expect(draft.suggestedFilename()).toBe("am-i-scholar-draft.txt");
 const [html]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export print-ready HTML"}).click()]);
 expect(html.suggestedFilename()).toBe("am-i-scholar-manuscript.html");
 const popupPromise=page.waitForEvent("popup");
 await page.getByRole("button",{name:"Print / Save PDF"}).click();
 const popup=await popupPromise;
 await expect(popup.locator("pre")).toContainText("A draft paragraph.");
 await page.getByRole("button",{name:"← Modules"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();
 await page.locator("#recordTitle").fill("Reference");
 await page.locator("#recordContent").fill("Authors: Author, A\nYear: 2024\nTitle: Source paper\nJournal / publisher: Journal\nDOI or official URL: 10.1000/abc");
 await page.getByRole("button",{name:"Save record"}).click();
 await page.locator("#cslStyle").selectOption("apa");
 await page.getByRole("button",{name:"Format bibliography offline"}).click();
 const [bib]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export CSL bibliography Markdown"}).click()]);
 expect(bib.suggestedFilename()).toBe("am-i-scholar-csl-bibliography.md");
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const [web]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export all web data"}).click()]);
 expect(web.suggestedFilename()).toBe("am-i-scholar-web-backup.json");
 const [android]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export selected paper for Android"}).click()]);
 expect(android.suggestedFilename()).toBe("am-i-scholar-android-project.json");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Calendar"}).click();
 await page.getByRole("button",{name:"Researcher Mode"}).click();
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const [student]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export student JSON"}).click()]);
 expect(student.suggestedFilename()).toBe("am-i-scholar-student-backup.json");
 const exported=JSON.parse(await fs.readFile(await student.path(),"utf8"));
 expect(exported).toMatchObject({format:"am-i-scholar-student",version:1});
 expect(exported.data).toMatchObject({courses:[],tasks:[],notes:[],decks:[],cards:[],reviews:[],sessions:[],calendarEvents:[],activeTimer:null});
});

test("BibTeX and RIS imports preserve common fields and export back to each format",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");await createPaper(page,"Bibliography portability");
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();
 page.on("dialog",dialog=>dialog.accept());
 let chooserPromise=page.waitForEvent("filechooser");
 await page.getByRole("button",{name:"Import BibTeX"}).click();
 let chooser=await chooserPromise;
 await chooser.setFiles({name:"sources.bib",mimeType:"application/x-bibtex",buffer:Buffer.from("@article{doe2024, author={Doe, Jane and Smith, John}, title={A {Robust} Study}, journal={Journal of Tests}, year={2024}, volume={8}, number={2}, pages={10--18}, doi={10.1234/test}}","utf8")});
 await expect(page.locator("#recordList")).toContainText("A Robust Study");
 chooserPromise=page.waitForEvent("filechooser");
 await page.getByRole("button",{name:"Import RIS"}).click();chooser=await chooserPromise;
 await chooser.setFiles({name:"sources.ris",mimeType:"application/x-research-info-systems",buffer:Buffer.from("TY  - JOUR\nID  - smith2023\nAU  - Smith, Alice\nTI  - RIS Sample Paper\nJO  - Research Journal\nPY  - 2023\nVL  - 5\nIS  - 1\nSP  - 20\nEP  - 29\nDO  - 10.5678/ris\nER  - ","utf8")});
 await expect(page.locator("#recordList")).toContainText("RIS Sample Paper");
 const [bib]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export BibTeX"}).click()]);
 expect(bib.suggestedFilename()).toBe("am-i-scholar-bibliography.bib");
 const bibText=await fs.readFile(await bib.path(),"utf8");expect(bibText).toContain("title = {A Robust Study}");expect(bibText).toContain("10.1234/test");expect(bibText).toContain("RIS Sample Paper");
 const [ris]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export RIS"}).click()]);
 expect(ris.suggestedFilename()).toBe("am-i-scholar-bibliography.ris");
 const risText=await fs.readFile(await ris.path(),"utf8");expect(risText).toContain("TI  - A Robust Study");expect(risText).toContain("DO  - 10.1234/test");expect(risText).toContain("TI  - RIS Sample Paper");
});

test("citation metadata warnings stay separate from unchanged formatted output",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");await createPaper(page,"Citation warnings");
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();
 await page.locator("#recordTitle").fill("Minimal Evidence");await page.locator("#recordContent").fill("Title: Minimal Evidence");
 await page.getByRole("button",{name:"Save record"}).click();
 await expect(page.locator("#citationWarnings")).toContainText("Missing author");
 await expect(page.locator("#citationWarnings")).toContainText("Missing year");
 await page.getByRole("button",{name:"Format bibliography offline"}).click();
 await expect(page.locator("#cslResult")).toContainText("Minimal Evidence");
 await expect(page.locator("#cslResult")).not.toContainText("Missing author");
});

test("local PDF reader saves a page highlight and note with the paper",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");await createPaper(page,"PDF reader annotations");
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();
 const file={name:"reading-sample.pdf",mimeType:"application/pdf",buffer:await fs.readFile("tests/fixtures/pdf-reader-sample.pdf"),lastModified:123456};
 await page.locator("#pdfFile").setInputFiles(file);
 await expect(page.locator("#pdfTextLayer")).toContainText("Highlight this sentence.",{timeout:15000});
 const selected=await page.locator("#pdfTextLayer").evaluate((root,needle)=>{const walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),nodes=[];let all="",node;while((node=walker.nextNode())){nodes.push({node,start:all.length,end:all.length+node.textContent.length});all+=node.textContent}const begin=all.indexOf(needle);if(begin<0)return false;const finish=begin+needle.length,a=nodes.find(x=>begin>=x.start&&begin<x.end),b=nodes.find(x=>finish>x.start&&finish<=x.end);if(!a||!b)return false;const range=document.createRange();range.setStart(a.node,begin-a.start);range.setEnd(b.node,finish-b.start);const selection=window.getSelection();selection.removeAllRanges();selection.addRange(range);return true},"Highlight this sentence.");
 expect(selected).toBeTruthy();await page.locator("#pdfHighlightNote").fill("Check the reported mechanism.");
 await expect(page.getByRole("button",{name:"Save selected text as highlight"})).toBeEnabled();
 await page.getByRole("button",{name:"Save selected text as highlight"}).click();
 await expect(page.locator("#pdfSavedHighlights")).toContainText("Highlight this sentence.");
 await expect(page.locator("#pdfSavedHighlights")).toContainText("Check the reported mechanism.");
 await expect(page.locator("#pdfTextLayer .pdf-saved-highlight")).toBeVisible();
 await page.reload();await page.locator("aside").getByRole("button",{name:/My papers/}).click();await page.getByRole("button",{name:"Open paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();await page.locator("#pdfFile").setInputFiles(file);
 await expect(page.locator("#pdfSavedHighlights")).toContainText("Check the reported mechanism.");
 await expect(page.locator("#pdfTextLayer .pdf-saved-highlight")).toBeVisible();
});

const BackupCrypto=require("../backup-crypto.js");
test("encrypted research backup round-trips and rejects wrong passphrases or tampering",async()=>{
 const payload={format:"am-i-scholar-web",version:2,projects:[{id:"p1",title:"Local study"}],records:[],evidenceLinks:[]};
 const encrypted=await BackupCrypto.encrypt(payload,"correct horse battery staple");
 expect(JSON.parse(encrypted)).toMatchObject({format:"am-i-scholar-encrypted",version:1,kdf:"PBKDF2-HMAC-SHA-256",cipher:"AES-256-GCM"});
 expect(await BackupCrypto.decrypt(encrypted,"correct horse battery staple")).toEqual(payload);
 await expect(BackupCrypto.decrypt(encrypted,"incorrect horse battery staple")).rejects.toThrow();
 const edited=JSON.parse(encrypted);edited.version=2;
 await expect(BackupCrypto.decrypt(JSON.stringify(edited),"correct horse battery staple")).rejects.toThrow();
});

test("Android-compatible encrypted fixture decrypts in the browser implementation",async()=>{
 const fixture={format:"am-i-scholar-encrypted",version:1,kdf:"PBKDF2-HMAC-SHA-256",iterations:310000,cipher:"AES-256-GCM",salt:"AAECAwQFBgcICQoLDA0ODw==",iv:"AAECAwQFBgcICQoL",ciphertext:"cEXk3EJgR4JHR8OS/rppuK1Y5/b0NoNTsOmcinZMGDV4wFpa+go1paDP43s="};
 expect(await BackupCrypto.decrypt(JSON.stringify(fixture),"correct horse battery staple")).toEqual({fixture:"web-to-android"});
});

test("encrypted full backup exports and restores research plus student records",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await createPaper(page,"Encrypted full paper");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Courses"}).click();
 await page.getByRole("button",{name:"+ Add course"}).click();
 await page.locator('#academicBody input[name="name"]').fill("Encrypted course");
 await page.locator('#academicBody form button[type="submit"]').click();
 await page.getByRole("button",{name:"Researcher Mode"}).click();
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const downloadPromise=page.waitForEvent("download");
 await page.getByRole("button",{name:"Export encrypted full backup"}).click();
 const fields=page.locator('dialog input[type="password"]');
 await fields.nth(0).fill("sample passphrase for backup");
 await fields.nth(1).fill("sample passphrase for backup");
 await page.locator("dialog").getByRole("button",{name:"Encrypt"}).click();
 const download=await downloadPromise;
 const exported=await fs.readFile(await download.path(),"utf8");
 const envelope=JSON.parse(exported);
 expect(envelope.format).toBe("am-i-scholar-encrypted");
 expect(exported).not.toContain("Encrypted full paper");
 await page.evaluate(async()=>{
  localStorage.removeItem("amischolar.web.v2");
  await new Promise(resolve=>{const request=indexedDB.deleteDatabase("amischolar-academic-v1");request.onsuccess=request.onerror=request.onblocked=resolve});
 });
 await page.reload();
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 page.on("dialog",dialog=>dialog.accept());
 await page.locator("#restoreFile").setInputFiles({name:"encrypted-full.json",mimeType:"application/json",buffer:Buffer.from(exported)});
 const decryptFields=page.locator('dialog input[type="password"]');
 await decryptFields.fill("sample passphrase for backup");
 await page.locator("dialog").getByRole("button",{name:"Decrypt"}).click();
 await expect(page.locator("#status")).toHaveText("Research and student backup restored on this browser.");
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await expect(page.locator("#projectList")).toContainText("Encrypted full paper");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Courses"}).click();
 await expect(page.locator("#academicBody")).toContainText("Encrypted course");
});
