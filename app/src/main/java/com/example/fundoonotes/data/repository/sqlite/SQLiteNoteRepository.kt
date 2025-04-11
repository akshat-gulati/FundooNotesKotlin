package com.example.fundoonotes.data.repository.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SQLiteNoteRepository(context: Context): NotesInterface, SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    companion object{
        private const val TAG = "SQLiteRepository"
        private const val DATABASE_NAME = "notesapp.db"
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
            $COLUMN_DELETED INTEGER,
            $COLUMN_ARCHIVED INTEGER
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

        Log.d(TAG, "fetchNoteById not yet implemented")
    }

    override fun fetchNotes() {

    }

    override fun addNewNote(title: String, description: String, reminderTime: Long?): String {
        Log.d(TAG, "addNewNote not yet implemented")
        return ""
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        Log.d(TAG, "updateNote not yet implemented")
    }

    override fun deleteNote(noteId: String) {
        Log.d(TAG, "deleteNote not yet implemented")
    }


}