package com.example.fundoonotes.data.repository.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.fundoonotes.data.model.Label
import kotlin.String

@Entity(tableName = "labels")
data class LabelEntity (
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val noteIds: String = "", // Store as String in DB
) {
    fun toLabel(): Label {
        return Label(
            id = id,
            name = name,
            noteIds = if (noteIds.isEmpty()) emptyList() else noteIds.split(",")
        )
    }

    companion object {
        fun fromLabel(label: Label): LabelEntity {
            return LabelEntity(
                id = label.id,
                name = label.name,
                noteIds = label.noteIds.joinToString(",")
            )
        }
    }
}

class Converters {
    @TypeConverter
    fun fromListToString(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromStringToList(string: String): List<String> {
        return if (string.isEmpty()) emptyList() else string.split(",")
    }
}