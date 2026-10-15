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
