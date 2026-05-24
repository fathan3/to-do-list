package com.example.data

import androidx.room.TypeConverter
import com.example.data.model.Subtask
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val subtaskListType = Types.newParameterizedType(List::class.java, Subtask::class.java)
    private val adapter = moshi.adapter<List<Subtask>>(subtaskListType)

    @TypeConverter
    fun fromSubtaskList(value: List<Subtask>?): String {
        return adapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toSubtaskList(value: String?): List<Subtask> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
