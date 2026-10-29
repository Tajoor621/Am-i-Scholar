const {test,expect}=require("@playwright/test");

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

 page.on("dialog",dialog=>dialog.accept());
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 await page.locator("#restoreFile").setInputFiles({name:"old-web.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify({format:"am-i-scholar-web",version:1,projects:[{id:"old-project",title:"Version one"}],records:[]}))});
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await expect(page.locator("#projectList")).toContainText("Version one");
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const android={format:"am-i-scholar-project",version:1,project:{title:"Android import",researchQuestion:"A question?",discipline:"Biology",citationStyle:"APA"},notes:[{title:"Method",content:"Local note",kind:"DRAFT"}],sources:[{title:"Source",authors:"A. Author",year:"2023",doiOrUrl:"10.1000/doi",publication:"Journal",verified:false}],evidenceLinks:[]};
 await page.locator("#restoreFile").setInputFiles({name:"android.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify(android))});
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await expect(page.locator("#projectList")).toContainText("Android import");
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const webV2={format:"am-i-scholar-web",version:2,projects:[{id:"v2-project",title:"Version two",question:"Stored question?",discipline:"Biology",citationStyle:"APA"}],records:[{id:"v2-record",projectId:"v2-project",moduleId:"draft-studio",title:"Restored draft",content:"Preserved text",updated:1}],evidenceLinks:[]};
 await page.locator("#restoreFile").setInputFiles({name:"current-web.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify(webV2))});
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
 expect(student.suggestedFilename()).toBe("am-i-scholar-academic-backup.json");
});
