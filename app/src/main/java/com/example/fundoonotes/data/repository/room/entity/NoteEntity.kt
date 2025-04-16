package com.example.fundoonotes.data.repository.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.fundoonotes.data.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val labels: String, // Stored as comma-separated string
    val deleted: Boolean,
    val archived: Boolean,
    val reminderTime: Long?,
    val deletedTime: Long?
) {
    // Converter function to convert NoteEntity to Note
    fun toNote(): Note {
        return Note(
            id = id,
            userId = userId,
            title = title,
            description = description,
            timestamp = timestamp,
            labels = if (labels.isEmpty()) emptyList() else labels.split(","),
            deleted = deleted,
            archived = archived,
            reminderTime = reminderTime,
            deletedTime = deletedTime
        )
    }

    companion object {
        // Converter function to convert Note to NoteEntity
        fun fromNote(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                userId = note.userId,
                title = note.title,
                description = note.description,
                timestamp = note.timestamp,
                labels = note.labels.joinToString(","),
                deleted = note.deleted,
                archived = note.archived,
                reminderTime = note.reminderTime,
                deletedTime = note.deletedTime
            )
        }
    }
}

// Type converters for Room
class Converters {
    @TypeConverter
    fun fromBoolean(value: Boolean): Int {
        return if (value) 1 else 0
    }

    @TypeConverter
    fun toBoolean(value: Int): Boolean {
        return value == 1
    }
}