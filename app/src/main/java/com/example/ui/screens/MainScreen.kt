package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.JournalEntry
import com.example.data.TaskItem
import com.example.ui.viewmodel.JournalUiState
import com.example.ui.viewmodel.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Launch snackbar notifications upon successful writes or errors
    LaunchedEffect(uiState) {
        if (uiState is JournalUiState.Success) {
            snackbarHostState.showSnackbar((uiState as JournalUiState.Success).message)
            viewModel.resetUiState()
        } else if (uiState is JournalUiState.Error) {
            snackbarHostState.showSnackbar("Error: ${(uiState as JournalUiState.Error).exception}")
            viewModel.resetUiState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "App Icon Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Smart Planner",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Journal") },
                    label = { Text("Journal") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Tasks List") },
                    label = { Text("Tasks") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics & Tips") },
                    label = { Text("Insights") }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Create an abstract, premium gradient background
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF141F30), // Cosmic dark theme base colors
                                Color(0xFF0F141D)
                            )
                        )
                    )
                }
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut(animationSpec = tween(220)))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> JournalTab(viewModel)
                    1 -> TasksTab(viewModel)
                    2 -> InsightsTab(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalTab(viewModel: MainViewModel) {
    val entries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isWriting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isWriting) {
            // Journal Creation Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Write Daily Thought",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("What's on your mind? (Gemini will auto-plan tasks)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                isWriting = false
                                title = ""
                                content = ""
                            },
                            modifier = Modifier.height(48.dp) // Accessibility min target
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (title.isNotBlank() && content.isNotBlank()) {
                                    viewModel.addJournalEntry(title, content)
                                    title = ""
                                    content = ""
                                    isWriting = false
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = title.isNotBlank() && content.isNotBlank() && uiState !is JournalUiState.Loading,
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (uiState is JournalUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Sparkle",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Analyze & Save")
                            }
                        }
                    }
                }
            }
        } else {
            // Display standard CTA to write thoughts
            Button(
                onClick = { isWriting = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Create, contentDescription = "Write logo")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write New Journal Entry", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Header for Entries log List
        Text(
            text = "Journal History Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Empty Journal",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Mind Map is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Take a moment to write a journal entry below. Gemini AI will analyze your mood and generate smart tasks for your daily schedule!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(entries) { entry ->
                    JournalItemCard(entry, onDelete = { viewModel.deleteJournalEntry(entry) })
                }
            }
        }
    }
}

@Composable
fun JournalItemCard(entry: JournalEntry, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val moodColor = when (entry.mood) {
        "Happy" -> Color(0xFFFFF176) // Soft Yellow
        "Calm" -> Color(0xFF81C784) // Soft Green
        "Balanced" -> Color(0xFF4DB6AC) // Teal
        "Inspired" -> Color(0xFFFFB74D) // Amber Orange
        "Sad" -> Color(0xFF64B5F6) // Sky Blue
        "Anxious" -> Color(0xFFE57373) // Coral Red
        "Tired" -> Color(0xFFBA68C8) // Lavender Purple
        else -> Color(0xFF90A4AE) // Slate Gray (Neutral)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // First Row: Date, Title, Mood Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateFormat.format("MMM dd, yyyy - hh:mm a", Date(entry.timestamp)).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Mood tag bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(moodColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.mood,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = moodColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Short Preview
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                textAlign = TextAlign.Start
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // AI summary block inside the card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI response graphic icon",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Column {
                        Text(
                            text = "AI Mindful Summary & Impact",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = entry.aiSummary ?: "No Summary available offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Delete Entry Trigger Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp) // Touch targets >= 48dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Journal Log",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand logs dropdown",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TasksTab(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var taskName by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var showPriorityMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Task Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Custom Action Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = taskName,
                        onValueChange = { taskName = it },
                        label = { Text("Task description...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Button(
                            onClick = { showPriorityMenu = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text(priority)
                        }
                        DropdownMenu(
                            expanded = showPriorityMenu,
                            onDismissRequest = { showPriorityMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("High") },
                                onClick = {
                                    priority = "High"
                                    showPriorityMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Medium") },
                                onClick = {
                                    priority = "Medium"
                                    showPriorityMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Low") },
                                onClick = {
                                    priority = "Low"
                                    showPriorityMenu = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (taskName.isNotBlank()) {
                            viewModel.addTask(taskName, priority)
                            taskName = ""
                        }
                    },
                    enabled = taskName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal Icon")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tasks Header
        Text(
            text = "Active Schedule",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Empty tasks",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Everything complete!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Write journals to unlock direct AI action item extractions, or log items manually on this screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tasks) { task ->
                    TaskCardItem(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCardItem(
    task: TaskItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "High" -> Color(0xFFE57373)
        "Medium" -> Color(0xFFFFB74D)
        else -> Color(0xFF81C784)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = if (task.isCompleted) 0.5f else 0.85f
            )
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for task completion status
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(48.dp) // Accessibility min target
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = if (task.isCompleted) {
                            MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                        }
                    )

                    if (task.isAiGenerated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Generated Sparkle Indicator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Priority Indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(priorityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${task.priority} Priority",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = priorityColor
                        )
                    }

                    if (task.isAiGenerated) {
                        Text(
                            text = "Generated by AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp) // Touch targets >= 48dp
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun InsightsTab(viewModel: MainViewModel) {
    val entries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val weeklyInsights by viewModel.weeklyInsights.collectAsStateWithLifecycle()
    val insightsLoading by viewModel.insightsLoading.collectAsStateWithLifecycle()

    var triggerCheck by remember { mutableStateOf(false) }

    // Run first time if entries changed
    LaunchedEffect(triggerCheck) {
        if (entries.isNotEmpty()) {
            viewModel.loadWeeklyInsights()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom Mood Trends Chart Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mood Balance Radar (7 Entries)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Record entries in the Journal tab to trace emotional metrics.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Custom Canvas Line chart plotting Sentiment scores
                        val reversedEntries = remember(entries) { entries.take(7).reversed() }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            val scoreStepX = size.width / (if (reversedEntries.size > 1) (reversedEntries.size - 1) else 1)
                            val chartScaleY = size.height / 5f // Max sentiment score is 5

                            // Draw reference grid gridlines
                            for (gridLineIdx in 1..4) {
                                val gridY = size.height - (gridLineIdx * (size.height / 5f))
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.15f),
                                    start = Offset(0f, gridY),
                                    end = Offset(size.width, gridY),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Build sentiment line path
                            val graphLinePath = Path()
                            reversedEntries.forEachIndexed { elementIdx, journalEntry ->
                                val scoreVal = journalEntry.sentimentScore.coerceIn(1.0f, 5.0f)
                                val dotX = elementIdx * scoreStepX
                                val dotY = size.height - (scoreVal * chartScaleY)

                                if (elementIdx == 0) {
                                    graphLinePath.moveTo(dotX, dotY)
                                } else {
                                    graphLinePath.lineTo(dotX, dotY)
                                }

                                // Plot data Node circle overlay
                                drawCircle(
                                    color = when (journalEntry.mood) {
                                        "Happy" -> Color(0xFFFFF176)
                                        "Calm" -> Color(0xFF81C784)
                                        "Balanced" -> Color(0xFF4DB6AC)
                                        "Inspired" -> Color(0xFFFFB74D)
                                        "Sad" -> Color(0xFF64B5F6)
                                        "Anxious" -> Color(0xFFE57373)
                                        "Tired" -> Color(0xFFBA68C8)
                                        else -> Color(0xFF90A4AE)
                                    },
                                    radius = 6.dp.toPx(),
                                    center = Offset(dotX, dotY)
                                )
                            }

                            if (reversedEntries.size > 1) {
                                drawPath(
                                    path = graphLinePath,
                                    color = Color(0xFF4DB6AC),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }

                        // Bottom labels indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val displayDotsCount = reversedEntries.size
                            reversedEntries.forEachIndexed { index, item ->
                                Text(
                                    text = item.mood.take(4),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weekly spiritual insights response
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Advisor Trend Reflection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = { triggerCheck = !triggerCheck },
                            enabled = !insightsLoading,
                            modifier = Modifier.size(48.dp) // Touch targets >= 48dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh wellness insights tracker panel"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (insightsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Text(
                            text = weeklyInsights.ifEmpty {
                                "Record personal logs first, then tap the refresh button to trigger Gemini AI trend counseling reports."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Mandatory Secret Key warning Banner under our android-secret-management skill
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "APK Key caution notification banner icon",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = "Security Advisor Flag",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

// Simple CardStroke utility helper
@Composable
fun CardStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
