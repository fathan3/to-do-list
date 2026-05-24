package com.example.data

import com.example.data.model.TodoTask
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    val allTasks: Flow<List<TodoTask>> = todoDao.getAllTasks()

    suspend fun insertTask(task: TodoTask): Long {
        return todoDao.insertTask(task)
    }

    suspend fun updateTask(task: TodoTask) {
        todoDao.updateTask(task)
    }

    suspend fun deleteTask(task: TodoTask) {
        todoDao.deleteTask(task)
    }

    suspend fun deleteAllTasks() {
        todoDao.deleteAllTasks()
    }
}
