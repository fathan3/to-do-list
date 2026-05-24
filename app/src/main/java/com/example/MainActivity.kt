package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.TodoDatabase
import com.example.data.TodoRepository
import com.example.data.model.TodoTask
import com.example.ui.FilterStatus
import com.example.ui.SortOption
import com.example.ui.TodoViewModel
import com.example.ui.TodoViewModelFactory
import com.example.ui.components.AddTodoDialog
import com.example.ui.components.EditTodoDialog
import com.example.ui.components.ProgressRing
import com.example.ui.components.TaskCard
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Dynamic Request POST_NOTIFICATIONS permission for Android 13+
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean -> }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Init database manually
        val database = TodoDatabase.getDatabase(this)
        val repository = TodoRepository(database.todoDao)
        val factory = TodoViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[TodoViewModel::class.java]

        setContent {
            MyApplicationTheme {
                TodoScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val context = LocalContext.current

    // ViewModel states
    val tasks by viewModel.filteredTasks.collectAsState()
    val stats by viewModel.taskStats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPriority by viewModel.selectedPriority.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    // Screen tab selection: 0 = Tasks list, 1 = Calendar schedule, 2 = Premium stats dashboards
    var activeTab by remember { mutableStateOf(0) }

    // Dialog trigger states
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TodoTask?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val categoriesFilterList = remember { listOf("All", "General", "Personal", "Work", "Shopping", "Health") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "App logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (activeTab) {
                                1 -> "Kalender Deadline"
                                2 -> "Analisis & Statistik"
                                else -> "Todo List"
                            },
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Daftar Tugas") },
                    label = { Text("Tugas", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Kalender") },
                    label = { Text("Kalender", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Statistik") },
                    label = { Text("Statistik", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        },
        floatingActionButton = {
            if (activeTab != 2) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .testTag("add_task_fab")
                        .padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Task",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 0: THE COMPREHENSIVE TASKS CHEKLIST ENGINE
                    // 1. STATS OVERVIEW HEADER CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProgressRing(
                                progress = stats.completionRate,
                                size = 64.dp,
                                strokeWidth = 6.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selesai: ${stats.completedCount} dari ${stats.totalCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (stats.urgentCount > 0) {
                                        "⚠️ ${stats.urgentCount} Tugas Prioritas Tinggi aktif"
                                    } else {
                                        "✨ Kerja bagus! Kamu berada di jalur yang benar!"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (stats.urgentCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // 2. SEARCH & SORT BAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Cari daftar tugas...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("task_search_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier
                                    .testTag("sort_menu_btn")
                                    .size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.SortByAlpha,
                                    contentDescription = "Sorting options",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tgl Dibuat (Terbaru)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.CREATED_NEWEST)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Tgl Dibuat (Terlama)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.CREATED_OLDEST)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Deadline (Terdekat)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.DUE_DATE_ASC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Deadline (Terjauh)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.DUE_DATE_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Prioritas (Tinggi ke Rendah)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.PRIORITY_HIGH_FIRST)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Prioritas (Rendah ke Tinggi)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.PRIORITY_LOW_FIRST)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Abjad (A-Z)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.ALPHABETICAL_A_Z)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. STATUS FILTER TABS AND CLEAR BUTTON
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TabRow(
                            selectedTabIndex = filterStatus.ordinal,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            divider = {},
                            indicator = {}
                        ) {
                            FilterStatus.values().forEach { status ->
                                val isSelected = filterStatus == status
                                val indonesianLabel = when(status) {
                                    FilterStatus.ALL -> "Semua"
                                    FilterStatus.ACTIVE -> "Aktif"
                                    FilterStatus.COMPLETED -> "Selesai"
                                }
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.setFilterStatus(status) },
                                    modifier = Modifier.testTag("status_tab_${status.name.lowercase()}"),
                                    text = {
                                        Text(
                                            text = indonesianLabel,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }

                        if (stats.completedCount > 0) {
                            IconButton(
                                onClick = { viewModel.clearCompletedTasks(context) },
                                modifier = Modifier
                                    .testTag("clear_completed_btn")
                                    .size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Completed tasks",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // 4. CATEGORY HORIZONTAL ROW CHIPS
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoriesFilterList) { category ->
                            val isSelected = selectedCategory == category
                            val indonesianCategory = when(category) {
                                "All" -> "Semua Kategori"
                                "General" -> "Umum"
                                "Personal" -> "Pribadi"
                                "Work" -> "Pekerjaan"
                                "Shopping" -> "Belanja"
                                "Health" -> "Kesehatan"
                                else -> category
                            }
                            InputChip(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedCategory(category) },
                                modifier = Modifier.testTag("category_filter_${category.lowercase()}"),
                                label = { Text(indonesianCategory) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // 5. TO-DO TASK RECYCLER LIST / EMPTY STATE
                    if (tasks.isEmpty()) {
                        Spacer(modifier = Modifier.height(48.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.FilterList else Icons.Default.ListAlt,
                                contentDescription = "Empty states",
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Tidak ada tugas yang cocok" else "Semua Tugas Selesai!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    "Coba revisi kata sandi pencarian atau reset filter kategori/status."
                                } else {
                                    "Bagus sekali! Ketuk tanda tambah di pojok kanan bawah untuk menambahkan tugas baru."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("tasks_lazy_column")
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    onCheckedChange = { viewModel.toggleTaskComplete(context, task) },
                                    onDelete = { viewModel.deleteTodo(context, task) },
                                    onEdit = { taskToEdit = task },
                                    onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) },
                                    onAddSubtask = { subTitle -> viewModel.addSubtaskToExisting(task, subTitle) },
                                    onDeleteSubtask = { subId -> viewModel.deleteSubtaskFromExisting(task, subId) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp)) // Cushion for navigation and navigation padding
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: THE CALENDAR TIMELINE VIEW
                    val tasksWithDates = remember(tasks) {
                        tasks.filter { it.dueDate != null }.sortedBy { it.dueDate }
                    }
                    val sdfDay = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")) }

                    if (tasksWithDates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Empty Calendar",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Tidak Ada Batas Waktu Terdekat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Hebat! Tidak ada tugas aktif yang memiliki tenggat waktu atau deadline tersetting.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val grouped = remember(tasksWithDates) {
                            tasksWithDates.groupBy {
                                try {
                                    sdfDay.format(Date(it.dueDate!!))
                                } catch (e: Exception) {
                                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it.dueDate!!))
                                }
                            }
                        }
                        Text(
                            "Garis Waktu Tugas & Deadline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            grouped.forEach { (dateStr, taskList) ->
                                item {
                                    Card(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                items(taskList, key = { it.id }) { task ->
                                    TaskCard(
                                        task = task,
                                        onCheckedChange = { viewModel.toggleTaskComplete(context, task) },
                                        onDelete = { viewModel.deleteTodo(context, task) },
                                        onEdit = { taskToEdit = task },
                                        onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) },
                                        onAddSubtask = { subTitle -> viewModel.addSubtaskToExisting(task, subTitle) },
                                        onDeleteSubtask = { subId -> viewModel.deleteSubtaskFromExisting(task, subId) }
                                    )
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: DETAILED ANALYTICS DASHBOARD
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Analisis Produktivitas Anda",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Evaluasi performa dan tenggat waktu tugas secara terperinci.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Completion Metrics Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProgressRing(
                                        progress = stats.completionRate,
                                        size = 72.dp,
                                        strokeWidth = 8.dp
                                    )
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Column {
                                        Text(
                                            "Rasio Penyelesaian",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Sebanyak ${stats.completedCount} dari total ${stats.totalCount} tugas telah diselesaikan dengan sukses (${(stats.completionRate).toInt()}%).",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Categories breakdown bar chart representation
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Distribusi Kategori Tugas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (stats.categoryBreakdown.isEmpty()) {
                                        Text(
                                            "Belum ada data distribusi kategori.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    } else {
                                        stats.categoryBreakdown.forEach { (category, count) ->
                                            val percentage = if (stats.totalCount > 0) (count.toFloat() / stats.totalCount) else 0f
                                            val indonesianCategoryName = when(category) {
                                                "General" -> "Umum"
                                                "Personal" -> "Pribadi"
                                                "Work" -> "Pekerjaan"
                                                "Shopping" -> "Belanja"
                                                "Health" -> "Kesehatan"
                                                else -> category
                                            }
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        indonesianCategoryName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        "$count Tugas (${(percentage * 100).toInt()}%)",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = percentage,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Priority distribution visualization
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Diagram Prioritas Tugas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val priorityLevels = listOf(
                                        Triple(2, "Prioritas Tinggi (High)", MaterialTheme.colorScheme.error),
                                        Triple(1, "Prioritas Sedang (Medium)", MaterialTheme.colorScheme.primary),
                                        Triple(0, "Prioritas Rendah (Low)", MaterialTheme.colorScheme.outline)
                                    )
                                    priorityLevels.forEach { (level, name, color) ->
                                        val count = stats.priorityBreakdown[level] ?: 0
                                        val percentage = if (stats.totalCount > 0) (count.toFloat() / stats.totalCount) else 0f
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = color
                                                )
                                                Text(
                                                    "$count Tugas (${(percentage * 100).toInt()}%)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = color
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = percentage,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp),
                                                color = color,
                                                trackColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Productivity Tip / Banner info
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Weekly Tips",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "Tips Manajemen Deadline",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Gunakan pengingat alarm otomatis yang terjadwal H-1 hari, H-1 jam, dan tepat waktu deadline untuk meminimalkan keterlambatan tugas.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // 6. POPUP DIALOGS CONTROL
        if (showAddDialog) {
            AddTodoDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, desc, cat, priority, date, subs ->
                    viewModel.insertTask(
                        context = context,
                        title = title,
                        description = desc,
                        category = cat,
                        priority = priority,
                        dueDate = date,
                        subtasks = subs
                    )
                    showAddDialog = false
                }
            )
        }

        taskToEdit?.let { task ->
            EditTodoDialog(
                task = task,
                onDismiss = { taskToEdit = null },
                onConfirm = { updated ->
                    viewModel.updateTaskFull(context, updated)
                    taskToEdit = null
                }
            )
        }
    }
}
