package com.amischolar.app.data
import androidx.room.*
@Entity(tableName="research_notes", foreignKeys=[ForeignKey(entity=ResearchProject::class,parentColumns=["id"],childColumns=["projectId"],onDelete=ForeignKey.CASCADE)],indices=[Index("projectId")])
data class ResearchNote(@PrimaryKey(autoGenerate=true) val id:Long=0,val projectId:Long,val title:String,val content:String="",val kind:String="NOTE",val updatedAtEpochMillis:Long=System.currentTimeMillis())
