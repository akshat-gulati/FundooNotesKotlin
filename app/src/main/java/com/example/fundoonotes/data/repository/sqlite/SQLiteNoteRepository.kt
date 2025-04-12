package com.example.fundoonotes.data.repository.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SQLiteNoteRepository(context: Context): NotesInterface, SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    companion object{
        private const val TAG = "SQLiteRepository"
        private const val DATABASE_NAME = "FundooNotes.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "all_notes"

        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_LABELS = "labels"
        private const val COLUMN_DELETED = "deleted"
        private const val COLUMN_ARCHIVED = "archived"
        private const val COLUMN_REMINDER_TIME = "reminderTime"
        private const val COLUMN_DELETED_TIME = "deletedTime"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_ID TEXT PRIMARY KEY,
            $COLUMN_TITLE TEXT,
            $COLUMN_DESCRIPTION TEXT,
            $COLUMN_TIMESTAMP INTEGER,
            $COLUMN_LABELS TEXT,
            $COLUMN_DELETED INTEGER DEFAULT 0,
            $COLUMN_ARCHIVED INTEGER DEFAULT 0,
            $COLUMN_REMINDER_TIME INTEGER,
            $COLUMN_DELETED_TIME INTEGER
        )
    """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME"
        db?.execSQL(dropTableQuery)
        onCreate(db)
    }

    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(noteId))

        if (cursor.moveToFirst()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            val labelsString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LABELS))
            val deleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DELETED)) == 1
            val archived = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ARCHIVED)) == 1

            val reminderTimeIndex = cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME)
            val reminderTime = if (cursor.isNull(reminderTimeIndex)) null else cursor.getLong(reminderTimeIndex)

            val deletedTimeIndex = cursor.getColumnIndexOrThrow(COLUMN_DELETED_TIME)
            val deletedTime = if (cursor.isNull(deletedTimeIndex)) null else cursor.getLong(deletedTimeIndex)

            val labels = labelsString?.split(",")?.filter { it.isNotEmpty() } ?: listOf()

            val note = Note(
                id = id,
                title = title,
                description = description,
                timestamp = timestamp,
                labels = labels,
                deleted = deleted,
                archived = archived,
                reminderTime = reminderTime,
                deletedTime = deletedTime
            )

            onSuccess(note)
        }

        cursor.close()
        db.close()
    }

    override fun fetchNotes() {
        val noteList = mutableListOf<Note>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()){
            val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            val labelsString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LABELS))
            val deleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DELETED)) == 1
            val archived = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ARCHIVED)) == 1

            val reminderTimeIndex = cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME)
            val reminderTime = if (cursor.isNull(reminderTimeIndex)) null else cursor.getLong(reminderTimeIndex)

            val deletedTimeIndex = cursor.getColumnIndexOrThrow(COLUMN_DELETED_TIME)
            val deletedTime = if (cursor.isNull(deletedTimeIndex)) null else cursor.getLong(deletedTimeIndex)

            val labels = labelsString?.split(",")?.filter { it.isNotEmpty() } ?: listOf()

            val note = Note(
                id = id,
                title = title,
                description = description,
                timestamp = timestamp,
                labels = labels,
                deleted = deleted,
                archived = archived,
                reminderTime = reminderTime,
                deletedTime = deletedTime
            )

            noteList.add(note)
        }
        cursor.close()
        db.close()

        // Update the StateFlow with new data
        _notesState.value = noteList
    }

    override fun addNewNote(title: String, description: String, reminderTime: Long?): String {
        val db = writableDatabase
        val noteId = UUID.randomUUID().toString()

        val values = ContentValues().apply {
            put(COLUMN_ID, noteId)
            put(COLUMN_TITLE, title)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            put(COLUMN_LABELS, "")
            put(COLUMN_DELETED, 0)
            put(COLUMN_ARCHIVED, 0)
            if (reminderTime != null) {
                put(COLUMN_REMINDER_TIME, reminderTime)
            }
        }

        db.insert(TABLE_NAME, null, values)
        db.close()

        // Refresh notes after adding
        fetchNotes()

        return noteId
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_DESCRIPTION, description)
            if (reminderTime != null) {
                put(COLUMN_REMINDER_TIME, reminderTime)
            } else {
                putNull(COLUMN_REMINDER_TIME)
            }
        }

        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(noteId))
        db.close()

        // Refresh notes after updating
        fetchNotes()
    }

    override fun deleteNote(noteId: String) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(noteId))
        db.close()

        // Refresh notes after deleting
        fetchNotes()
    }

    // Additional methods to match FirestoreNoteRepository functionality

    fun updateNoteFields(noteId: String, fields: Map<String, Any?>) {
        val db = writableDatabase
        val values = ContentValues()

        fields.forEach { (key, value) ->
            when (key) {
                "title" -> if (value != null) values.put(COLUMN_TITLE, value as String)
                "description" -> if (value != null) values.put(COLUMN_DESCRIPTION, value as String)
                "labels" -> if (value != null) {
                    val labelsList = value as List<String>
                    values.put(COLUMN_LABELS, labelsList.joinToString(","))
                }
                "deleted" -> values.put(COLUMN_DELETED, if (value as Boolean) 1 else 0)
                "archived" -> values.put(COLUMN_ARCHIVED, if (value as Boolean) 1 else 0)
                "reminderTime" -> if (value != null) {
                    values.put(COLUMN_REMINDER_TIME, value as Long)
                } else {
                    values.putNull(COLUMN_REMINDER_TIME)
                }
                "deletedTime" -> if (value != null) {
                    values.put(COLUMN_DELETED_TIME, value as Long)
                } else {
                    values.putNull(COLUMN_DELETED_TIME)
                }
            }
        }

        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(noteId))
        db.close()

        // Refresh notes after updating fields
        fetchNotes()
    }

    fun getNoteById(noteId: String): Note? {
        var note: Note? = null
        fetchNoteById(noteId) {
            note = it
        }
        return note
    }
}