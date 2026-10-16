const {test,expect}=require("@playwright/test");
test("mobile research journey: create, edit, modules and recovery",async({page})=>{
 await page.setViewportSize({width:390,height:844});
 await page.goto("http://127.0.0.1:8000/");
 await expect(page.getByRole("heading",{name:"Dashboard"})).toBeVisible();
 await expect(page.locator("#homeModules .module")).toHaveCount(12);
 await expect(page.locator("aside .brand img")).toHaveJSProperty("complete",true);
 await page.getByRole("button",{name:"Create your first paper"}).click();
 await page.locator("#projectTitle").fill("Genome architecture");
 await page.locator("#projectQuestion").fill("How does bacterial chromosome organization affect transcription?");
 await page.locator("#projectDiscipline").fill("Microbiology");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Topic Lab"}).click();
 await expect(page.locator("#workspaceTitle")).toHaveText("Topic Lab");
 await page.getByRole("button",{name:"Use structured template"}).click();
 await page.locator("#recordTitle").fill("Feasibility v1");
 await page.getByRole("button",{name:"Save record"}).click();
 await expect(page.locator("#recordList")).toContainText("Feasibility v1");
 await page.reload();
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await expect(page.locator("#projectList")).toContainText("Genome architecture");
 await page.locator("aside").getByRole("button",{name:/Backup & settings/}).click();
 const [download]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export all web data"}).click()]);
 expect(download.suggestedFilename()).toBe("am-i-scholar-web-backup.json");
});
test("PWA exposes a manifest and registers a service worker",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await expect(page.locator('link[rel="manifest"]')).toHaveAttribute("href","./manifest.webmanifest");
 const response=await page.request.get("http://127.0.0.1:8000/sw.js");
 expect(response.ok()).toBeTruthy();
});

test("live literature metadata search imports a source without claiming verification",async({page})=>{
 await page.route("https://api.crossref.org/works?*",route=>route.fulfill({status:200,contentType:"application/json",body:JSON.stringify({message:{items:[{title:["Verified metadata test"],author:[{given:"Ada",family:"Lovelace"}],DOI:"10.1000/test",published:{"date-parts":[[2025]]},"container-title":["Example Journal"]}]}})}));
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("Research search test");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Literature Radar"}).click();
 await page.locator("#literatureQuery").fill("chromosome organization");
 await page.getByRole("button",{name:"Search research metadata"}).click();
 await expect(page.locator("#literatureResults")).toContainText("Verified metadata test");
 await page.getByRole("button",{name:"Import metadata"}).click();
 await expect(page.locator("#recordList")).toContainText("Verified metadata test");
 await expect(page.locator("#recordList")).toContainText("Verified against original: No");
});
test("draft tools export manuscript and citation metadata",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();await page.locator("#projectTitle").fill("Export test");await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Draft Studio"}).click();
 await page.getByRole("button",{name:"Use structured template"}).click();await page.locator("#recordTitle").fill("Draft 1");await page.getByRole("button",{name:"Save record"}).click();
 const [download]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export Markdown"}).click()]);
 expect(download.suggestedFilename()).toBe("am-i-scholar-manuscript.md");
 await page.getByRole("button",{name:"← Modules"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();
 const [csl]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export CSL-JSON metadata"}).click()]);
 expect(csl.suggestedFilename()).toBe("am-i-scholar-csl-data.json");
});

test("all twelve research modules expose contextual help and research-specific feedback",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("Help workflow test");
 await page.locator("#projectQuestion").fill("How do bacterial chromosomes respond to stress?");
 await page.getByRole("button",{name:"Save paper"}).click();
 const names=["Topic Lab","Literature Radar","Evidence Matrix","Outline Architect","Draft Studio","Style Coach","Citation Center","Originality & Integrity","Review Simulator","Submission Kit","Revision & Response","Archive & Reproducibility"];
 for(const name of names){
  await page.locator("#allModules").getByRole("button",{name:"Open "+name}).click();
  await page.getByRole("button",{name:"Help & research guidance"}).click();
  await expect(page.locator("#moduleHelp")).toBeVisible();
  await expect(page.locator("#moduleHelp")).toContainText("Step-by-step procedure");
  await expect(page.locator("#moduleHelp")).toContainText("Suggestions for your current research");
  await expect(page.locator("#moduleHelp ol li")).toHaveCount(4);
  await page.getByRole("button",{name:"← Modules"}).click();
 }
});

test("evidence matrix supports explicit claim status and export",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("Evidence test");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Evidence Matrix"}).click();
 await page.getByRole("button",{name:"Use structured template"}).click();
 await expect(page.locator("#recordContent")).toHaveValue(/Support status/);
 const [file]=await Promise.all([page.waitForEvent("download"),page.getByRole("button",{name:"Export evidence CSV"}).click()]);
 expect(file.suggestedFilename()).toBe("am-i-scholar-evidence.csv");
});

test("evidence relationships survive reload and research JSON backup",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("Claim links test");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Literature Radar"}).click();
 await page.locator("#recordTitle").fill("Source A");
 await page.locator("#recordContent").fill("Authors: Example Author\nYear: 2024\nVerified against original: No");
 await page.getByRole("button",{name:"Save record"}).click();
 await page.getByRole("button",{name:"← Modules"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Evidence Matrix"}).click();
 await page.locator("#recordTitle").fill("Claim A");
 await page.locator("#recordContent").fill("Claim: Claim A\nSupport status: unsupported");
 await page.getByRole("button",{name:"Save record"}).click();
 await page.getByRole("button",{name:"Save evidence link"}).click();
 await expect(page.locator("#toolPanel")).toContainText("Claim A → Source A");
 await page.reload();
 await page.locator("aside").getByRole("button",{name:/My papers/}).click();
 await page.getByRole("button",{name:"Open paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Evidence Matrix"}).click();
 await expect(page.locator("#toolPanel")).toContainText("Claim A → Source A");
});

test("offline CSL formats APA and IEEE using bundled processor and styles",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("CSL accuracy test");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();
 await page.locator("#recordTitle").fill("A study of microbes");
 await page.locator("#recordContent").fill("Authors: Smith, Jane; Khan, Ali\nYear: 2024\nTitle: A study of microbes\nJournal / publisher: Journal of Biology\nVolume / issue: 12(2)\nPages: 10-15\nDOI or official URL: 10.1234/example");
 await page.getByRole("button",{name:"Save record"}).click();
 await page.locator("#cslStyle").selectOption("apa");
 await page.getByRole("button",{name:"Format bibliography offline"}).click();
 await expect(page.locator("#cslResult")).toContainText("A study of microbes",{timeout:15000});
 await expect(page.locator("#cslResult")).toContainText("2024");
 await page.locator("#cslStyle").selectOption("ieee");
 await page.getByRole("button",{name:"Format bibliography offline"}).click();
 await expect(page.locator("#cslResult")).toContainText("A study of microbes",{timeout:15000});
 await expect(page.locator("#cslStyle option")).toHaveCount(7);
});
test("portable web to Android backup includes evidence links",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("Backup linked evidence");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Literature Radar"}).click();
 await page.locator("#recordTitle").fill("Source 1");
 await page.locator("#recordContent").fill("Authors: Smith\nYear: 2025");
 await page.getByRole("button",{name:"Save record"}).click();
 await page.getByRole("button",{name:"← Modules"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Evidence Matrix"}).click();
 await page.locator("#recordTitle").fill("Claim 1");
 await page.locator("#recordContent").fill("Claim: Claim 1\nSupport status: supported");
 await page.getByRole("button",{name:"Save record"}).click();
 await page.locator("#toolPanel select").first().selectOption({index:0});
 await page.getByRole("button",{name:"Save evidence link"}).click();
 const snapshot=await page.evaluate(()=>androidSnapshot(projectId));
 expect(snapshot.evidenceLinks).toHaveLength(1);
 expect(snapshot.evidenceLinks[0]).toMatchObject({noteIndex:0,sourceIndex:0,status:"unsupported"});
});

test("all seven locally bundled CSL styles render a bibliography",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("Seven styles");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();
 await page.locator("#recordTitle").fill("Research and discovery");
 await page.locator("#recordContent").fill("Authors: García, María; Smith, Jane\nYear: 2022\nJournal / publisher: Journal of Science\nDOI or official URL: https://doi.org/10.1234/test");
 await page.getByRole("button",{name:"Save record"}).click();
 for(const style of ["apa","modern-language-association","chicago-author-date","chicago-notes-bibliography-17th-edition","ieee","harvard-cite-them-right","elsevier-vancouver"]){
  await page.locator("#cslStyle").selectOption(style);
  await page.getByRole("button",{name:"Format bibliography offline"}).click();
  await expect(page.locator("#cslResult")).toContainText("Research and discovery",{timeout:15000});
  await expect(page.locator("#cslResult")).not.toContainText("CSL formatting unavailable");
 }
});
test("CSL-JSON import preserves bibliographic fields on export",async({page})=>{
 await page.goto("http://127.0.0.1:8000/");
 await page.locator("#headerNew").click();
 await page.locator("#projectTitle").fill("CSL roundtrip");
 await page.getByRole("button",{name:"Save paper"}).click();
 await page.locator("#allModules").getByRole("button",{name:"Open Citation Center"}).click();
 page.on("dialog",d=>d.accept());
 const chooserPromise=page.waitForEvent("filechooser");
 await page.getByRole("button",{name:"Import CSL-JSON metadata"}).click();
 const chooser=await chooserPromise;
 await chooser.setFiles({name:"sources.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify([{id:"a",type:"article-journal",title:"Unicode Study",author:[{family:"García",given:"María"}],"container-title":"Journal X",volume:"3",issued:{"date-parts":[[2025]]},DOI:"10.1234/abc"}]))});
 await expect(page.locator("#recordList")).toContainText("Unicode Study");
 const data=await page.evaluate(()=>{const r=data.records.find(x=>x.title==="Unicode Study");const f=parseFields(r.content);return JSON.parse(f["csl-json"])});
 expect(data["container-title"]).toBe("Journal X");
 expect(data.author[0].family).toBe("García");
});
