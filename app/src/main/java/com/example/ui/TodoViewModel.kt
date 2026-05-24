package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TodoRepository
import com.example.data.model.Subtask
import com.example.data.model.TodoTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class FilterStatus {
    ALL, ACTIVE, COMPLETED
}

enum class SortOption {
    DUE_DATE_ASC,
    DUE_DATE_DESC,
    PRIORITY_HIGH_FIRST,
    PRIORITY_LOW_FIRST,
    CREATED_NEWEST,
    CREATED_OLDEST,
    ALPHABETICAL_A_Z
}

data class TaskStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val completionRate: Float = 0f,
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val priorityBreakdown: Map<Int, Int> = emptyMap(),
    val urgentCount: Int = 0 // High priority & uncompleted
)

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {

    // Filtering & Sorting parameters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedPriority = MutableStateFlow<Int?> (null) // null = All
    val selectedPriority: StateFlow<Int?> = _selectedPriority.asStateFlow()

    private val _filterStatus = MutableStateFlow(FilterStatus.ALL)
    val filterStatus: StateFlow<FilterStatus> = _filterStatus.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.CREATED_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Base Stream of All Tasks
    val allTasks: StateFlow<List<TodoTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived Stream of Filtered and Sorted Tasks
    val filteredTasks: StateFlow<List<TodoTask>> = combine(
        allTasks,
        _searchQuery,
        _selectedCategory,
        combine(_selectedPriority, _filterStatus, _sortOption) { priority, status, sort ->
            Triple(priority, status, sort)
        }
    ) { tasks, query, cat, tuple ->
        val (priority, status, sort) = tuple
        var result = tasks

        // 1. Search Query
        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }

        // 2. Category Filter
        if (cat != "All") {
            result = result.filter { it.category.equals(cat, ignoreCase = true) }
        }

        // 3. Priority Filter
        if (priority != null) {
            result = result.filter { it.priority == priority }
        }

        // 4. Status Filter
        result = when (status) {
            FilterStatus.ALL -> result
            FilterStatus.ACTIVE -> result.filter { !it.isCompleted }
            FilterStatus.COMPLETED -> result.filter { it.isCompleted }
        }

        // 5. Sorting
        result = when (sort) {
            SortOption.DUE_DATE_ASC -> result.sortedWith { a, b ->
                val ad = a.dueDate ?: Long.MAX_VALUE
                val bd = b.dueDate ?: Long.MAX_VALUE
                ad.compareTo(bd)
            }
            SortOption.DUE_DATE_DESC -> result.sortedWith { a, b ->
                val ad = a.dueDate ?: Long.MIN_VALUE
                val bd = b.dueDate ?: Long.MIN_VALUE
                bd.compareTo(ad)
            }
            SortOption.PRIORITY_HIGH_FIRST -> result.sortedByDescending { it.priority }
            SortOption.PRIORITY_LOW_FIRST -> result.sortedBy { it.priority }
            SortOption.CREATED_NEWEST -> result.sortedByDescending { it.createdAt }
            SortOption.CREATED_OLDEST -> result.sortedBy { it.createdAt }
            SortOption.ALPHABETICAL_A_Z -> result.sortedBy { it.title.lowercase() }
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Derived Statistics
    val taskStats: StateFlow<TaskStats> = allTasks
        .combine(allTasks) { tasks, _ ->
            val total = tasks.size
            if (total == 0) return@combine TaskStats()

            val completed = tasks.count { it.isCompleted }
            val rate = (completed.toFloat() / total.toFloat()) * 100f

            val categoryMap = tasks.groupBy { it.category }.mapValues { it.value.size }
            val priorityMap = tasks.groupBy { it.priority }.mapValues { it.value.size }
            val urgent = tasks.count { !it.isCompleted && it.priority == 2 }

            TaskStats(
                totalCount = total,
                completedCount = completed,
                completionRate = rate,
                categoryBreakdown = categoryMap,
                priorityBreakdown = priorityMap,
                urgentCount = urgent
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TaskStats()
        )

    // Actions
    fun insertTask(
        context: Context,
        title: String,
        description: String,
        category: String,
        priority: Int,
        dueDate: Long?,
        subtasks: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val subtaskList = subtasks.filter { it.isNotBlank() }.map {
                Subtask(id = UUID.randomUUID().toString(), title = it, isCompleted = false)
            }
            val task = TodoTask(
                title = title,
                description = description,
                category = category,
                priority = priority,
                dueDate = dueDate,
                isCompleted = false,
                subtasks = subtaskList
            )
            val generatedId = repository.insertTask(task)
            if (dueDate != null) {
                com.example.util.NotificationHelper.scheduleTaskReminders(
                    context,
                    generatedId.toInt(),
                    title,
                    dueDate
                )
            }
        }
    }

    fun updateTaskFull(context: Context, task: TodoTask) {
        viewModelScope.launch {
            repository.updateTask(task)
            com.example.util.NotificationHelper.cancelTaskReminders(context, task.id)
            if (!task.isCompleted && task.dueDate != null) {
                com.example.util.NotificationHelper.scheduleTaskReminders(
                    context,
                    task.id,
                    task.title,
                    task.dueDate
                )
            }
        }
    }

    fun toggleTaskComplete(context: Context, task: TodoTask) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                // Automatically toggle subtasks depending on parent status
                subtasks = task.subtasks.map { it.copy(isCompleted = !task.isCompleted) }
            )
            repository.updateTask(updated)
            
            // Cancel reminders if completed, or reschedule if toggled to active
            com.example.util.NotificationHelper.cancelTaskReminders(context, task.id)
            if (!updated.isCompleted && updated.dueDate != null) {
                com.example.util.NotificationHelper.scheduleTaskReminders(
                    context,
                    updated.id,
                    updated.title,
                    updated.dueDate
                )
            }
        }
    }

    fun deleteTodo(context: Context, task: TodoTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            com.example.util.NotificationHelper.cancelTaskReminders(context, task.id)
        }
    }

    fun toggleSubtask(task: TodoTask, subtaskId: String) {
        viewModelScope.launch {
            val updatedSubtasks = task.subtasks.map {
                if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
            }
            val allSubTasksDone = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it.isCompleted }
            val updatedTask = task.copy(
                subtasks = updatedSubtasks,
                isCompleted = if (allSubTasksDone) true else task.isCompleted
            )
            repository.updateTask(updatedTask)
        }
    }

    fun addSubtaskToExisting(task: TodoTask, subtaskTitle: String) {
        if (subtaskTitle.isBlank()) return
        viewModelScope.launch {
            val newSub = Subtask(id = UUID.randomUUID().toString(), title = subtaskTitle, isCompleted = false)
            val updatedTask = task.copy(
                subtasks = task.subtasks + newSub,
                isCompleted = false
            )
            repository.updateTask(updatedTask)
        }
    }

    fun deleteSubtaskFromExisting(task: TodoTask, subtaskId: String) {
        viewModelScope.launch {
            val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
            val updatedTask = task.copy(subtasks = updatedSubtasks)
            repository.updateTask(updatedTask)
        }
    }

    fun clearCompletedTasks(context: Context) {
        viewModelScope.launch {
            allTasks.value.filter { it.isCompleted }.forEach {
                repository.deleteTask(it)
                com.example.util.NotificationHelper.cancelTaskReminders(context, it.id)
            }
        }
    }

    // Setters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedPriority(priority: Int?) {
        _selectedPriority.value = priority
    }

    fun setFilterStatus(status: FilterStatus) {
        _filterStatus.value = status
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
}

class TodoViewModelFactory(private val repository: TodoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
