package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.GeminiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class JournalUiState {
    object Idle : JournalUiState()
    object Loading : JournalUiState()
    data class Success(val message: String) : JournalUiState()
    data class Error(val exception:String) : JournalUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository

    val journalEntries: StateFlow<List<JournalEntry>>
    val tasks: StateFlow<List<TaskItem>>

    private val _uiState = MutableStateFlow<JournalUiState>(JournalUiState.Idle)
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private val _weeklyInsights = MutableStateFlow<String>("")
    val weeklyInsights: StateFlow<String> = _weeklyInsights.asStateFlow()

    private val _insightsLoading = MutableStateFlow<Boolean>(false)
    val insightsLoading: StateFlow<Boolean> = _insightsLoading.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.journalDao(), database.taskDao())
        
        journalEntries = repository.allEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        tasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addJournalEntry(title: String, content: String) {
        if (content.isBlank() || title.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = JournalUiState.Loading
            try {
                // Call Gemini for analysis (Structured analysis of sentiment and extracted tasks)
                val analysisResult = GeminiApiClient.analyzeJournalEntry(content)
                
                // Construct the journal entry
                val journalEntry = JournalEntry(
                    title = title,
                    content = content,
                    mood = analysisResult.mood,
                    sentimentScore = analysisResult.sentimentScore,
                    aiSummary = analysisResult.summary
                )
                
                // Save Journal to Room Database and get back its unique row id
                val journalId = repository.insertJournal(journalEntry)
                
                // Automatically generate and insert outstanding sub-tasks to the Planner!
                for (extractedTaskTitle in analysisResult.tasks) {
                    val taskItem = TaskItem(
                        parentJournalId = journalId.toInt(),
                        title = extractedTaskTitle,
                        priority = when {
                            extractedTaskTitle.contains("critical", ignoreCase = true) || 
                            extractedTaskTitle.contains("important", ignoreCase = true) -> "High"
                            else -> "Medium"
                        },
                        isCompleted = false,
                        isAiGenerated = true
                    )
                    repository.insertTask(taskItem)
                }
                
                _uiState.value = JournalUiState.Success("Journal entered with ${analysisResult.tasks.size} smart action goals generated.")
            } catch (e: Exception) {
                // Network failure or API Key missing backup: Save locally offline cleanly
                val offlineEntry = JournalEntry(
                    title = title,
                    content = content,
                    mood = "Neutral",
                    sentimentScore = 3.0f,
                    aiSummary = "Saved locally. Configure your GEMINI_API_KEY to unlock advanced sentiment analysis and automated sub-task lists."
                )
                val journalId = repository.insertJournal(offlineEntry)
                
                // Add minor default tasks
                repository.insertTask(
                    TaskItem(
                        parentJournalId = journalId.toInt(),
                        title = "Reflect on your journal entry: '$title'",
                        priority = "Medium",
                        isCompleted = false,
                        isAiGenerated = true
                    )
                )
                
                _uiState.value = JournalUiState.Success("Saved locally. (Offline state or Gemini key missing)")
            }
        }
    }

    fun deleteJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteJournal(entry)
        }
    }

    fun addTask(title: String, priority: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = TaskItem(
                title = title,
                priority = priority,
                isCompleted = false,
                isAiGenerated = false
            )
            repository.insertTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun loadWeeklyInsights() {
        viewModelScope.launch {
            _insightsLoading.value = true
            try {
                val currentEntries = journalEntries.value
                val textsList = currentEntries.take(5).map { 
                    "[Title: ${it.title}] [Mood: ${it.mood}] ${it.content}" 
                }
                val insights = GeminiApiClient.generateWeeklyInsights(textsList)
                _weeklyInsights.value = insights
            } catch (e: Exception) {
                _weeklyInsights.value = "Unable to compile trends: ${e.localizedMessage}"
            } finally {
                _insightsLoading.value = false
            }
        }
    }
    
    fun resetUiState() {
        _uiState.value = JournalUiState.Idle
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
