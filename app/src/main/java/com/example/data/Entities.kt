package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: String = "Neutral", // Happy, Calm, Neutral, Sad, Anxious, Inspired
    val sentimentScore: Float = 3.0f, // 1 to 5 mapping
    val aiSummary: String? = null
)

@Entity(tableName = "task_items")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parentJournalId: Int? = null, // Links task back to a specific journal entry if AI-generated
    val title: String,
    val priority: String = "Medium", // High, Medium, Low
    val isCompleted: Boolean = false,
    val isAiGenerated: Boolean = false,
    val dueDate: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)
