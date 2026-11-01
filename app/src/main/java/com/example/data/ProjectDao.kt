package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM whiteboard_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<WhiteboardProjectEntity>>

    @Query("SELECT * FROM whiteboard_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): WhiteboardProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: WhiteboardProjectEntity)

    @Query("DELETE FROM whiteboard_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}
