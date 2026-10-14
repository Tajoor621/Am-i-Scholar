package com.amischolar.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "research_projects")
data class ResearchProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val researchQuestion: String = "",
    val discipline: String = "Not set",
    val citationStyle: String = "APA",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)
