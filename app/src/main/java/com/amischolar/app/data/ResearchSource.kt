package com.amischolar.app.data
import androidx.room.*
@Entity(tableName="research_sources",foreignKeys=[ForeignKey(entity=ResearchProject::class,parentColumns=["id"],childColumns=["projectId"],onDelete=ForeignKey.CASCADE)],indices=[Index("projectId")])
data class ResearchSource(@PrimaryKey(autoGenerate=true) val id:Long=0,val projectId:Long,val title:String,val authors:String="",val year:String="",val doiOrUrl:String="",val publication:String="",val evidenceNotes:String="",val verified:Boolean=false)
