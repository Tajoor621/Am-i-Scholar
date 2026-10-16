package com.amischolar.app.data

import androidx.room.*

/** Researcher-authored relationship. Status is an assessment, never automatic verification. */
@Entity(
    tableName = "evidence_links",
    foreignKeys = [
        ForeignKey(entity = ResearchProject::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ResearchNote::class, parentColumns = ["id"], childColumns = ["claimNoteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ResearchSource::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("projectId"), Index("claimNoteId"), Index("sourceId")]
)
data class EvidenceLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val claimNoteId: Long,
    val sourceId: Long,
    val status: String = "unsupported",
    val rationale: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    init { require(status in setOf("supported", "partially supported", "unsupported")) }
}
