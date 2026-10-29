const {test,expect}=require("@playwright/test");
const fs=require("node:fs/promises");
test("student mode keeps existing researcher modules and supports course, task, notes and deck",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await expect(page.locator("#academicWorkspace")).toBeVisible();
 await page.locator("#academicNav").getByRole("button",{name:"Courses"}).click();
 await page.getByRole("button",{name:"+ Add course"}).click();
 await page.locator('#academicBody input[name="name"]').fill("Molecular Biology");
 await page.locator('#academicBody input[name="code"]').fill("BIO301");
 await page.locator('#academicBody form button[type="submit"]').click();
 await expect(page.locator("#academicBody")).toContainText("Molecular Biology");
 await page.locator("#academicNav").getByRole("button",{name:"Tasks"}).click();
 await page.getByRole("button",{name:"+ New task"}).click();
 await page.locator('#academicBody input[name="title"]').fill("Lab report");
 await page.locator('#academicBody form button[type="submit"]').click();
 await expect(page.locator("#academicBody")).toContainText("Lab report");
 await page.locator("#academicNav").getByRole("button",{name:"Notes"}).click();
 await page.getByRole("button",{name:"+ New note"}).click();
 await page.locator('#academicBody input[name="title"]').fill("Cell division");
 await page.locator('#academicBody textarea[name="content"]').fill("# Notes\nResearch summary");
 await page.locator('#academicBody form button[type="submit"]').click();
 await expect(page.locator("#academicBody")).toContainText("Cell division");
 await page.reload();
 await expect(page.locator("#academicWorkspace")).toBeVisible();
 await page.locator("#academicNav").getByRole("button",{name:"Courses"}).click();
 await expect(page.locator("#academicBody")).toContainText("Molecular Biology");
 await page.getByRole("button",{name:"Researcher Mode"}).click();
 await expect(page.locator("#homeModules .module")).toHaveCount(12);
});
test("student flashcard study updates review history offline",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Flashcards"}).click();
 await page.getByRole("button",{name:"+ New deck"}).click();
 await page.locator('#academicBody input[name="title"]').fill("Genetics");
 await page.locator('#academicBody form button[type="submit"]').click();
 await page.getByRole("button",{name:"Add card"}).click();
 await page.locator('#academicBody textarea[name="front"]').fill("What is DNA?");
 await page.locator('#academicBody textarea[name="back"]').fill("Deoxyribonucleic acid");
 await page.locator('#academicBody form button[type="submit"]').click();
 await page.getByRole("button",{name:"Study due"}).click();
 await page.getByRole("button",{name:"Reveal answer"}).click();
 await page.getByRole("button",{name:"good",exact:true}).click();
 await expect(page.locator("#academicBody")).toContainText("No cards due right now.");
});

test("student calendar imports timezone-aware ICS events and exports them offline",async({browser})=>{
 const context=await browser.newContext({timezoneId:"Asia/Dubai",acceptDownloads:true});
 const page=await context.newPage();
 await page.goto("http://127.0.0.1:8000/");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Calendar"}).click();
 await page.locator("#calendarIcsFile").setInputFiles({name:"student-timezones.ics",mimeType:"text/calendar",buffer:await fs.readFile("tests/fixtures/student-calendar-timezones.ics")});
 await expect(page.locator("#calendarIcsStatus")).toContainText("4 calendar event(s) imported.");
 await page.locator('#academicBody input[type="month"]').fill("2026-03");
 await page.locator("#academicBody h2").click();
 await expect(page.locator("#academicBody")).toContainText("Spring-forward seminar");
 await expect(page.locator("#academicBody")).toContainText("10:30 AM");
 await expect(page.locator("#academicBody")).toContainText("All-day study fair");
 await page.locator("#calendarIcsFile").setInputFiles({name:"student-timezones.ics",mimeType:"text/calendar",buffer:await fs.readFile("tests/fixtures/student-calendar-timezones.ics")});
 await expect(page.locator("#calendarIcsStatus")).toContainText("4 calendar event(s) imported.");
 await page.locator('#academicBody input[type="month"]').fill("2026-03");await page.locator("#academicBody h2").click();
 await expect(page.locator("#academicBody .record").filter({hasText:"Spring-forward seminar"})).toHaveCount(1);
 const [download]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export ICS"}).click()]);
 expect(download.suggestedFilename()).toBe("am-i-scholar-calendar.ics");
 const exported=await fs.readFile(await download.path(),"utf8");
 expect(exported).toContain("SUMMARY:Spring-forward seminar");
 expect(exported).toContain("DTSTART;VALUE=DATE:20260309");
 expect(exported.match(/BEGIN:VEVENT/g)).toHaveLength(4);
 await context.close();
});

test("focus timer state and course, paper, and task links survive reload and are logged locally",async({page})=>{
 page.on("console",message=>{if(message.type()==="error")console.log("Browser console:",message.text())});
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("Focus-linked paper");
 await page.getByRole("button",{name:"Save paper"}).click();
 const paperId=await page.evaluate(()=>JSON.parse(localStorage.getItem("amischolar.web.v2")).projects[0].id);
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Courses"}).click();
 await page.getByRole("button",{name:"+ Add course"}).click();
 await page.locator('#academicBody input[name="name"]').fill("Biology 301");
 await page.locator('#academicBody form button[type="submit"]').click();
 await page.locator("#academicNav").getByRole("button",{name:"Tasks"}).click();
 await page.getByRole("button",{name:"+ New task"}).click();
 await page.locator('#academicBody input[name="title"]').fill("Cell report");
 await page.locator('#academicBody select[name="courseId"]').selectOption({label:"Biology 301"});
 await page.locator('#academicBody form button[type="submit"]').click();
 const taskId=await page.evaluate(async()=>{const db=await new Promise((resolve,reject)=>{const request=indexedDB.open("amischolar-academic-v1");request.onsuccess=()=>resolve(request.result);request.onerror=()=>reject(request.error)});return await new Promise((resolve,reject)=>{const request=db.transaction("tasks").objectStore("tasks").getAll();request.onsuccess=()=>resolve(request.result[0].id);request.onerror=()=>reject(request.error)})});
 const courseKey=await page.evaluate(async()=>{const db=await new Promise(resolve=>{const request=indexedDB.open("amischolar-academic-v1");request.onsuccess=()=>resolve(request.result)});return await new Promise(resolve=>{const request=db.transaction("courses").objectStore("courses").getAll();request.onsuccess=()=>resolve(request.result[0].id)})});
 await page.locator("#academicNav").getByRole("button",{name:"Focus"}).click();
 await expect(page.locator("#academicBody")).toContainText("Pomodoro focus",{timeout:8000});
 await page.locator('#academicBody select[name="focusCourse"]').selectOption(courseKey);
 await page.locator('#academicBody select[name="focusPaper"]').selectOption(String(paperId));
 await page.locator('#academicBody select[name="focusTask"]').selectOption(taskId);
 await page.locator("#focusMinutes").fill("1");
 await page.getByRole("button",{name:"Start",exact:true}).click();
 await expect(page.locator("#focusTimerStatus")).toContainText("saved on this device");
 await page.getByRole("button",{name:"Pause / resume"}).click();
 await expect(page.locator("#focusTimerStatus")).toContainText("paused and saved");
 await page.reload();
 await page.locator("#academicNav").getByRole("button",{name:"Focus"}).click();
 await expect(page.locator("#focusClock")).not.toHaveText("25:00");
 await page.getByRole("button",{name:"Pause / resume"}).click();
 await expect(page.locator("#focusTimerStatus")).toContainText("resumed");
 await page.getByRole("button",{name:"Finish & log"}).click();
 await expect(page.locator("#academicBody")).toContainText("Study time · last seven days");
 await expect(page.locator("#academicBody")).toContainText("1 day local study streak");
 await expect(page.locator("#academicBody")).toContainText("Biology 301");
 await expect(page.locator("#academicBody")).toContainText("Focus-linked paper");
 await expect(page.locator("#academicBody")).toContainText("Cell report");
 const session=await page.evaluate(async()=>{const db=await new Promise(resolve=>{const request=indexedDB.open("amischolar-academic-v1");request.onsuccess=()=>resolve(request.result)});return await new Promise(resolve=>{const request=db.transaction("sessions").objectStore("sessions").getAll();request.onsuccess=()=>resolve(request.result[0])})});
 expect(session).toMatchObject({courseId:courseKey,paperId:String(paperId),taskId});
});

test("student workspace restores the shared web and Android backup shape",async({page})=>{
 const backup={format:"am-i-scholar-student",version:1,exportedAt:"2026-10-28T18:00:00Z",data:{
  courses:[{id:"course-web-1",name:"History 201",code:"HIS201",semester:"Fall",createdAt:"2026-10-01T00:00:00Z",updatedAt:"2026-10-01T00:00:00Z"}],
  tasks:[{id:"task-web-1",title:"Primary source essay",type:"todo",priority:2,dueDate:"2026-11-03",courseId:"course-web-1",paperId:null,description:"Draft",status:"todo",createdAt:"2026-10-01T00:00:00Z",updatedAt:"2026-10-01T00:00:00Z"}],
  notes:[{id:"note-web-1",title:"Lecture notes",content:"## Week one",courseId:"course-web-1",paperId:null,tags:"week1",updatedAt:"2026-10-01T00:00:00Z"}],
  decks:[{id:"deck-web-1",title:"Key terms",courseId:"course-web-1",paperId:null,createdAt:"2026-10-01T00:00:00Z"}],
  cards:[{id:"card-web-1",deckId:"deck-web-1",front:"What is a primary source?",back:"Direct evidence from the period.",repetitions:1,easeFactor:2.5,intervalDays:1,dueAt:"2026-10-29T00:00:00Z",updatedAt:"2026-10-28T00:00:00Z"}],
  reviews:[{id:"review-web-1",cardId:"card-web-1",rating:"good",reviewedAt:"2026-10-28T00:00:00Z"}],
  sessions:[{id:"session-web-1",startedAt:"2026-10-28T16:00:00Z",endedAt:"2026-10-28T16:25:00Z",durationSeconds:1500,courseId:"course-web-1",paperId:null,taskId:"task-web-1",notes:""}],
  calendarEvents:[{uid:"event-web-1",title:"History seminar",description:"Room 2",allDay:false,startDate:"",endDateExclusive:"",startAt:"2026-10-29T10:00:00Z",endAt:"2026-10-29T11:00:00Z",timeZone:"UTC",source:"ics"}],activeTimer:null
 }};
 page.on("dialog",dialog=>dialog.accept());
 await page.goto("http://127.0.0.1:8000/");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.getByRole("button",{name:/Researcher Mode/}).click();
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 await page.locator('#settingsPage input[type="file"]').last().setInputFiles({name:"shared-student-backup.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify(backup))});
 await expect(page.getByRole("button",{name:"Student Mode"})).toBeVisible();
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Tasks"}).click();
 await expect(page.locator("#academicBody")).toContainText("Primary source essay");
 const restored=await page.evaluate(async()=>{const db=await new Promise((resolve,reject)=>{const req=indexedDB.open("amischolar-academic-v1");req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(req.error)});const get=store=>new Promise((resolve,reject)=>{const req=db.transaction(store).objectStore(store).getAll();req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(req.error)});return {course:await get("courses"),task:await get("tasks"),card:await get("cards"),review:await get("reviews"),session:await get("sessions"),event:await get("milestones")}});
 expect(restored.course[0].id).toBe("course-web-1");
 expect(restored.task[0].courseId).toBe("course-web-1");
 expect(restored.card[0].deckId).toBe("deck-web-1");
 expect(restored.review[0].cardId).toBe("card-web-1");
 expect(restored.session[0].taskId).toBe("task-web-1");
 expect(restored.event[0]).toMatchObject({uid:"event-web-1",kind:"ics-event"});
 expect(typeof restored.event[0].startAt).toBe("number");
});

test("source-grounded AI handoff requires one-time consent and saves a separate local disclosure log",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.evaluate(()=>localStorage.setItem("amischolar.web.v2",JSON.stringify({
  version:2,projects:[{id:"p-ai",title:"Local project",question:"private project question"}],
  records:[
   {id:"source-ai",projectId:"p-ai",moduleId:"literature-radar",title:"Selected Study",content:"Authors: A. Researcher\\nYear: 2024\\nDOI or official URL: https://doi.org/10.test/selected\\nVerified against original: Yes\\nAbstract summary (your own words): Selected finding."},
   {id:"source-other",projectId:"p-ai",moduleId:"literature-radar",title:"Unselected secret",content:"DOI or official URL: https://secret.test/1\\nAbstract summary (your own words): secret evidence."},
   {id:"draft-private",projectId:"p-ai",moduleId:"draft-studio",title:"Private draft",content:"never include this draft"}
  ],evidenceLinks:[]
 })));
 await page.reload();
 await page.getByRole("button",{name:"Open Draft Studio"}).click();
 await expect(page.locator("#aiCopyPrompt")).toBeDisabled();
 await expect(page.locator("#aiPromptPreview")).not.toContainText("private project question");
 await expect(page.locator("#aiPromptPreview")).not.toContainText("never include this draft");
 await page.locator("#ai-source-source-ai").check();
 await expect(page.locator("#aiPromptPreview")).toContainText("Selected Study");
 await expect(page.locator("#aiPromptPreview")).not.toContainText("Unselected secret");
 await expect(page.locator("#aiCopyPrompt")).toBeDisabled();
 await page.locator("#aiShareConsent").check();
 await expect(page.locator("#aiCopyPrompt")).toBeEnabled();
 await page.locator("#aiCopyPrompt").click();
 await expect(page.locator("#aiHandoffStatus")).toContainText("this app has not sent it");
 await expect(page.locator("#aiShareConsent")).not.toBeChecked();
 await page.locator("#aiProvider").fill("External test provider");
 await page.locator("#aiResponse").fill("An unverified response with [S1].");
 await page.locator("#aiReviewed").check();
 await page.locator("#aiSaveLog").click();
 const stored=await page.evaluate(()=>JSON.parse(localStorage.getItem("amischolar.web.v2")));
 const log=stored.records.find(r=>r.moduleId==="archive"&&r.title.startsWith("AI-use log"));
 expect(log.content).toContain("Selected source IDs: source-ai");
 expect(log.content).toContain("External test provider");
 expect(log.content).toContain("An unverified response");
 expect(log.content).toContain("The app did not verify the AI output.");
 expect(stored.records.find(r=>r.id==="draft-private").content).toBe("never include this draft");
 await page.locator("#backWorkspace").click();
 await page.getByRole("button",{name:"Open Archive & Reproducibility"}).click();
 const [download]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export this paper for Android"}).click()]);
 const projectBackup=JSON.parse(await fs.readFile(await download.path(),"utf8"));
 const backedLog=projectBackup.notes.find(n=>n.kind==="ARCHIVE"&&n.title.startsWith("AI-use log"));
 expect(backedLog.content).toContain("External test provider");
 expect(backedLog.content).toContain("An unverified response");
 expect(backedLog.content).toContain("Selected source IDs: source-ai");
});

test("lecture capture is local, editable, persistent and deletable",async({page})=>{
 const external=[];
 page.on("request",request=>{if(!request.url().startsWith("http://127.0.0.1:8000"))external.push(request.url())});
 await page.addInitScript(()=>{
  const track={stop(){}};
  Object.defineProperty(navigator,"mediaDevices",{configurable:true,value:{getUserMedia:async()=>({getTracks:()=>[track]})}});
  window.MediaRecorder=class{
   static isTypeSupported(){return true}
   constructor(_stream,options={}){this.mimeType=options.mimeType||"audio/webm";this.state="inactive"}
   start(){this.state="recording";setTimeout(()=>this.ondataavailable?.({data:new Blob(["local audio"],{type:this.mimeType})}),20)}
   stop(){this.state="inactive";this.onstop?.()}
  }
 });
 await page.goto("http://127.0.0.1:8000/");
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Lectures"}).click();
 await page.getByLabel("Recording title").fill("Cell Biology");
 await page.getByRole("button",{name:"Start recording"}).click();
 await expect(page.locator("#academicWorkspace [role=status]")).toBeVisible();
 await page.locator("#academicNav").getByRole("button",{name:"Tasks"}).click();
 await expect(page.locator("#academicWorkspace [role=status]")).toBeVisible();
 await page.getByRole("button",{name:"Stop lecture capture"}).click();
 await expect(page.getByText(/Recording saved locally/)).toBeVisible();
 const transcript=page.getByLabel("Transcript for Cell Biology");
 await transcript.fill("Mitochondria produce ATP.");
 await page.getByRole("button",{name:"Save transcript"}).click();
 await expect(page.getByText("Transcript saved locally.")).toBeVisible();
 await page.reload();
 await page.getByRole("button",{name:"Student Mode"}).click();
 await page.locator("#academicNav").getByRole("button",{name:"Lectures"}).click();
 await expect(page.getByLabel("Transcript for Cell Biology")).toHaveValue("Mitochondria produce ATP.");
 expect(external).toEqual([]);
 page.on("dialog",dialog=>dialog.accept());
 await page.getByRole("button",{name:"Delete",exact:true}).click();
 await expect(page.getByText("No recordings saved yet.")).toBeVisible();
});
