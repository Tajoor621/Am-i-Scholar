"use strict";
/* Small, dependency-free RFC 5545 calendar subset for explicit offline imports/exports. */
(function attachCalendarICS(root){
 const pad=value=>String(value).padStart(2,"0");
 const asDateKey=parts=>`${parts.year}-${pad(parts.month)}-${pad(parts.day)}`;
 const validParts=parts=>{
  const date=new Date(Date.UTC(parts.year,parts.month-1,parts.day,parts.hour||0,parts.minute||0,parts.second||0));
  return date.getUTCFullYear()===parts.year&&date.getUTCMonth()+1===parts.month&&date.getUTCDate()===parts.day&&date.getUTCHours()===(parts.hour||0)&&date.getUTCMinutes()===(parts.minute||0)&&date.getUTCSeconds()===(parts.second||0);
 };
 function wallTimeToEpoch(parts,timeZone){
  const target=Date.UTC(parts.year,parts.month-1,parts.day,parts.hour||0,parts.minute||0,parts.second||0);
  const formatter=new Intl.DateTimeFormat("en-CA-u-ca-gregory-nu-latn",{timeZone,year:"numeric",month:"2-digit",day:"2-digit",hour:"2-digit",minute:"2-digit",second:"2-digit",hourCycle:"h23"});
  let guess=target;
  for(let attempt=0;attempt<5;attempt++){
   const values=Object.fromEntries(formatter.formatToParts(new Date(guess)).filter(part=>part.type!=="literal").map(part=>[part.type,Number(part.value)]));
   const observed=Date.UTC(values.year,values.month-1,values.day,values.hour,values.minute,values.second);
   const delta=observed-target;
   if(delta===0)return guess;
   guess-=delta;
  }
  /* A nonexistent wall time during a DST jump is normalized forward by the transition. */
  const fields=formatter.formatToParts(new Date(guess));
  if(fields.length)return guess;
  throw new Error("Invalid date-time in timezone "+timeZone);
 }
 function parseDateProperty(property,fallbackTimezone){
  if(!property) return null;
  const raw=property.value.trim(),params=property.params||{};
  if(params.VALUE?.toUpperCase()==="DATE"||/^\d{8}$/.test(raw)){
   if(!/^\d{8}$/.test(raw))throw new Error("Invalid all-day date: "+raw);
   const parts={year:+raw.slice(0,4),month:+raw.slice(4,6),day:+raw.slice(6,8)};
   if(!validParts(parts))throw new Error("Invalid all-day date: "+raw);
   return {allDay:true,date:asDateKey(parts)};
  }
  const match=raw.match(/^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})?(Z)?$/i);
  if(!match)throw new Error("Unsupported calendar date-time: "+raw);
  const parts={year:+match[1],month:+match[2],day:+match[3],hour:+match[4],minute:+match[5],second:+(match[6]||0)};
  if(!validParts(parts))throw new Error("Invalid calendar date-time: "+raw);
  const tz=params.TZID?.replace(/^\"|\"$/g,"")||"";
  let epoch;
  if(match[7])epoch=Date.UTC(parts.year,parts.month-1,parts.day,parts.hour,parts.minute,parts.second);
  else if(tz)epoch=wallTimeToEpoch(parts,tz);
  else if(fallbackTimezone)epoch=wallTimeToEpoch(parts,fallbackTimezone);
  else epoch=new Date(parts.year,parts.month-1,parts.day,parts.hour,parts.minute,parts.second).getTime();
  if(!Number.isFinite(epoch))throw new Error("Invalid calendar date-time: "+raw);
  return {allDay:false,epoch,timeZone:tz||fallbackTimezone||""};
 }
 const unescapeText=value=>value.replace(/\\[nN]/g,"\n").replace(/\\([\\,;])/g,"$1");
 function parseProperty(line){
  const colon=line.indexOf(":");if(colon<1)return null;
  const [name,...rawParams]=line.slice(0,colon).split(";");const params={};
  for(const item of rawParams){const index=item.indexOf("=");if(index>0)params[item.slice(0,index).toUpperCase()]=item.slice(index+1)}
  return {name:name.toUpperCase(),params,value:line.slice(colon+1)};
 }
 function addDays(dateKey,amount){const [year,month,day]=dateKey.split("-").map(Number),date=new Date(Date.UTC(year,month-1,day+amount));return asDateKey({year:date.getUTCFullYear(),month:date.getUTCMonth()+1,day:date.getUTCDate()})}
 function parseICS(input,{fallbackTimezone=""}={}){
  const physical=String(input||"").replace(/^\uFEFF/,"").replace(/\r\n?/g,"\n").split("\n"),lines=[];
  for(const line of physical){if(/^[ \t]/.test(line)&&lines.length)lines[lines.length-1]+=line.slice(1);else lines.push(line)}
  const events=[],warnings=[];let current=null,ignoredDepth=0;
  for(const line of lines){const p=parseProperty(line);if(!p)continue;
   if(p.name==="BEGIN"&&p.value.toUpperCase()==="VEVENT"){current={properties:[],nested:0};ignoredDepth=0;continue}
   if(!current)continue;
   if(p.name==="BEGIN"){current.nested++;ignoredDepth++;continue}
   if(p.name==="END"&&p.value.toUpperCase()==="VEVENT"&&current.nested===0){
    const values={};for(const item of current.properties)values[item.name]=item;
    try{
     const start=parseDateProperty(values.DTSTART,fallbackTimezone);if(!start)throw new Error("Event is missing DTSTART");
     const finish=parseDateProperty(values.DTEND,fallbackTimezone);
     const uid=unescapeText(values.UID?.value||"").trim();
     const title=unescapeText(values.SUMMARY?.value||"Untitled calendar event").trim();
     let event;
     if(start.allDay){const end=finish?.allDay?finish.date:addDays(start.date,1);event={uid,title,description:unescapeText(values.DESCRIPTION?.value||""),allDay:true,startDate:start.date,endDateExclusive:end,timeZone:""}}
     else {const end=finish&&!finish.allDay?finish.epoch:start.epoch+3600000;event={uid,title,description:unescapeText(values.DESCRIPTION?.value||""),allDay:false,startAt:start.epoch,endAt:end,timeZone:start.timeZone||finish?.timeZone||""}}
     const rule=values.RRULE?.value;if(rule)warnings.push({uid,title,message:"Repeating events are imported as their first occurrence only."});
     events.push(event);
    }catch(error){warnings.push({uid:unescapeText(values.UID?.value||""),message:error.message||"Calendar event could not be read."})}
    current=null;continue;
   }
   if(p.name==="END"){current.nested=Math.max(0,current.nested-1);ignoredDepth=Math.max(0,ignoredDepth-1);continue}
   if(!ignoredDepth)current.properties.push(p);
  }
  const seen=new Set(),unique=[];
  for(const event of events){const key=event.uid||`${event.title}|${event.startAt||event.startDate}`;if(seen.has(key))continue;seen.add(key);unique.push(event)}
  unique.sort((a,b)=>(a.startAt??a.startDate).toString().localeCompare((b.startAt??b.startDate).toString()));
  return {events:unique,warnings};
 }
 const escapeText=value=>String(value||"").replace(/\\/g,"\\\\").replace(/\r?\n/g,"\\n").replace(/;/g,"\\;").replace(/,/g,"\\,");
 function foldLine(line){let out="",part="",bytes=0;for(const character of line){const size=new TextEncoder().encode(character).length;if(bytes+size>75){out+=part+"\r\n ";part=character;bytes=1+size}else{part+=character;bytes+=size}}return out+part}
 function utcValue(epoch){const date=new Date(epoch);return `${date.getUTCFullYear()}${pad(date.getUTCMonth()+1)}${pad(date.getUTCDate())}T${pad(date.getUTCHours())}${pad(date.getUTCMinutes())}${pad(date.getUTCSeconds())}Z`}
 function serializeICS(events){
  const lines=["BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//Am I Scholar//Student Tools//EN","CALSCALE:GREGORIAN"];
  for(const [index,event] of (events||[]).entries()){
   const start=event.allDay?event.startDate:event.startAt;
   if(!start)continue;
   lines.push("BEGIN:VEVENT",`UID:${escapeText(event.uid||`amischolar-${index}-${String(start)}`)}`,`DTSTAMP:${utcValue(Date.now())}`,`SUMMARY:${escapeText(event.title||"Untitled calendar event")}`);
   if(event.description)lines.push(`DESCRIPTION:${escapeText(event.description)}`);
   if(event.allDay){lines.push(`DTSTART;VALUE=DATE:${String(event.startDate).replace(/-/g,"")}`,`DTEND;VALUE=DATE:${String(event.endDateExclusive||addDays(event.startDate,1)).replace(/-/g,"")}`)}
   else {lines.push(`DTSTART:${utcValue(Number(event.startAt))}`,`DTEND:${utcValue(Number(event.endAt||Number(event.startAt)+3600000))}`)}
   if(event.status)lines.push(`STATUS:${String(event.status).toUpperCase()==="DONE"?"COMPLETED":"CONFIRMED"}`);
   lines.push("END:VEVENT");
  }
  lines.push("END:VCALENDAR");return lines.map(foldLine).join("\r\n")+"\r\n";
 }
 function formatWhen(event,timeZone=Intl.DateTimeFormat().resolvedOptions().timeZone){
  if(event.allDay)return new Intl.DateTimeFormat(undefined,{timeZone:"UTC",dateStyle:"medium"}).format(new Date(`${event.startDate}T00:00:00Z`))+" · all day";
  return new Intl.DateTimeFormat(undefined,{timeZone,dateStyle:"medium",timeStyle:"short"}).format(new Date(event.startAt));
 }
 const api={parseICS,serializeICS,formatWhen,wallTimeToEpoch,addDays};
 if(typeof module!=="undefined"&&module.exports)module.exports=api;
 root.StudentCalendarICS=api;
})(typeof globalThis!=="undefined"?globalThis:this);
