package com.amischolar.app.feature
import com.amischolar.app.data.ResearchSource
/** Draft bibliography only. For publication use a verified CSL processor and check source metadata. */
object CitationFormatter {
 val styles=listOf("APA","MLA","Chicago","IEEE","Harvard","Vancouver")
 fun format(s:ResearchSource,style:String,index:Int=1):String {
  val author=s.authors.ifBlank{"[Author not supplied]"}
  val year=s.year.ifBlank{"n.d."}
  val title=s.title.ifBlank{"[Title not supplied]"}
  val publication=s.publication.takeIf{it.isNotBlank()}?.let{" $it."}?:""
  val locator=s.doiOrUrl.takeIf{it.isNotBlank()}?.let{" $it"}?:""
  return when(style){
   "APA"->"$author ($year). $title.$publication$locator"
   "MLA"->"$author. \"$title.\"$publication $year.$locator"
   "Chicago"->"$author. \"$title.\"$publication $year.$locator"
   "IEEE"->"[$index] $author, \"$title,\"$publication $year.$locator"
   "Harvard"->"$author ($year) '$title'.$publication$locator"
   "Vancouver"->"$index. $author. $title.$publication $year.$locator"
   else->format(s,"APA",index)
  }
 }
 fun bibtex(s:ResearchSource,index:Int):String {
  fun clean(t:String)=t.replace("{","").replace("}","").replace("\n"," ")
  return "@misc{source$index,\n  title = {"+clean(s.title)+"},\n  author = {"+clean(s.authors)+"},\n  year = {"+clean(s.year)+"},\n  url = {"+clean(s.doiOrUrl)+"}\n}"
 }
 fun ris(s:ResearchSource):String = buildString {
  appendLine("TY  - GEN")
  appendLine("TI  - "+s.title.replace("\n"," "))
  s.authors.split(';').map{it.trim()}.filter{it.isNotEmpty()}.forEach{appendLine("AU  - "+it)}
  if(s.year.isNotBlank())appendLine("PY  - "+s.year)
  if(s.publication.isNotBlank())appendLine("T2  - "+s.publication)
  if(s.doiOrUrl.startsWith("10."))appendLine("DO  - "+s.doiOrUrl)
  else if(s.doiOrUrl.isNotBlank())appendLine("UR  - "+s.doiOrUrl)
  appendLine("ER  -")
 }
}
