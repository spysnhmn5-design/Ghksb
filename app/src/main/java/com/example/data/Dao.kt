package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry): Long

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_items ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM task_items WHERE parentJournalId = :journalId")
    fun getTasksForJournal(journalId: Int): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem)

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM task_items WHERE parentJournalId = :journalId")
    suspend fun deleteTasksForJournal(journalId: Int)
}
