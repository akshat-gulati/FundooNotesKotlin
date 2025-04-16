package com.example.fundoonotes.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.fundoonotes.data.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "labels")
    val labels: String,  // Stored as comma-separated string

    @ColumnInfo(name = "deleted")
    val deleted: Boolean,

    @ColumnInfo(name = "archived")
    val archived: Boolean,

    @ColumnInfo(name = "reminderTime")
    val reminderTime: Long?,

    @ColumnInfo(name = "deletedTime")
    val deletedTime: Long?
) {
    fun toNote(): Note {
        return Note(
            id = id,
            title = title,
            description = description,
            timestamp = timestamp,
            labels = labels.split(",").filter { it.isNotEmpty() },
            deleted = deleted,
            archived = archived,
            reminderTime = reminderTime,
            deletedTime = deletedTime
        )
    }

    companion object {
        fun fromNote(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
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
