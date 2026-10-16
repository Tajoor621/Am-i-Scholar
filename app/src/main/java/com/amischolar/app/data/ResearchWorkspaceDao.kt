package com.amischolar.app.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao interface ResearchWorkspaceDao {
 @Query("SELECT * FROM research_notes WHERE projectId=:projectId ORDER BY updatedAtEpochMillis DESC") fun observeNotes(projectId:Long):Flow<List<ResearchNote>>
 @Insert suspend fun insertNote(note:ResearchNote):Long
 @Update suspend fun updateNote(note:ResearchNote)
 @Query("DELETE FROM research_notes WHERE id=:id") suspend fun deleteNote(id:Long)
 @Query("SELECT * FROM research_sources WHERE projectId=:projectId ORDER BY id DESC") fun observeSources(projectId:Long):Flow<List<ResearchSource>>
 @Insert suspend fun insertSource(source:ResearchSource):Long
 @Update suspend fun updateSource(source:ResearchSource)
 @Query("DELETE FROM research_sources WHERE id=:id") suspend fun deleteSource(id:Long)
 @Query("SELECT * FROM evidence_links WHERE projectId=:projectId ORDER BY createdAtEpochMillis DESC") fun observeEvidenceLinks(projectId:Long):Flow<List<EvidenceLink>>
 @Query("SELECT * FROM evidence_links WHERE projectId=:projectId ORDER BY id") suspend fun getEvidenceLinks(projectId:Long):List<EvidenceLink>
 @Insert suspend fun insertEvidenceLink(link:EvidenceLink):Long
 @Query("DELETE FROM evidence_links WHERE id=:id") suspend fun deleteEvidenceLink(id:Long)
}
