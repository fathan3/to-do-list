package com.example.data.model

import androidx.annotation.Keep

@Keep
data class Subtask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)
