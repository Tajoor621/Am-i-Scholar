(function(){
 "use strict";
 const timers=new Map();
 function key(task){return "amischolar.task-reminder."+task.id+"."+task.reminderAt}
 function fire(task){
  const marker=key(task);
  if(localStorage.getItem(marker)||!task.reminderAt)return;
  if("Notification" in window&&Notification.permission==="granted"){
   new Notification("Study task reminder",{body:task.title||"A task is due.",tag:"amischolar-task-"+task.id});
   localStorage.setItem(marker,"1");
  }
 }
 function schedule(tasks){
  for(const timer of timers.values())clearTimeout(timer);timers.clear();
  for(const task of tasks){
   if(task.status==="done"||!task.reminderAt)continue;
   const at=Date.parse(task.reminderAt);if(!Number.isFinite(at))continue;
   const marker=key(task);if(localStorage.getItem(marker))continue;
   const arm=()=>{const wait=Math.max(0,at-Date.now()),timer=setTimeout(()=>{if(at-Date.now()>0){arm();return}timers.delete(task.id);fire(task)},Math.min(wait,2147480000));timers.set(task.id,timer)};
   arm();
  }
 }
 async function requestPermission(){
  if(!("Notification" in window))return "unsupported";
  return Notification.requestPermission();
 }
 window.StudentTaskReminders={schedule,requestPermission};
})();
