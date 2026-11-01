package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whiteboard_projects")
data class WhiteboardProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val updatedAt: Long,
    val pageCount: Int,
    val pagesJson: String
)
