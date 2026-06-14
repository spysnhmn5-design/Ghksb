package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val journalDao: JournalDao,
    private val taskDao: TaskDao
) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()
    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()

    suspend fun insertJournal(entry: JournalEntry): Long {
        return journalDao.insertEntry(entry)
    }

    suspend fun deleteJournal(entry: JournalEntry) {
        // Also delete sub-tasks associated with this journal entry when deleted
        taskDao.deleteTasksForJournal(entry.id)
        journalDao.deleteEntry(entry)
    }

    suspend fun insertTask(task: TaskItem) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskItem) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskItem) {
        taskDao.deleteTask(task)
    }

    fun getTasksForJournal(journalId: Int): Flow<List<TaskItem>> {
        return taskDao.getTasksForJournal(journalId)
    }
}
