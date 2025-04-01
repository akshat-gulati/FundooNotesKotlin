package com.example.fundoonotes.ui.noteEdit

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fundoonotes.R
import com.example.fundoonotes.data.repository.FirestoreNoteRepository
import com.example.fundoonotes.data.repository.NotesDataBridge

class NoteEditActivity : AppCompatActivity() {
    private lateinit var ivBack: ImageView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
//    private lateinit var firestoreNoteRepository: FirestoreNoteRepository
private lateinit var notesDataBridge: NotesDataBridge




    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_note_edit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize repository with application context
//        firestoreNoteRepository = FirestoreNoteRepository(applicationContext)
        notesDataBridge = NotesDataBridge(applicationContext)

        initializeViews()

        // Get note ID from intent
        noteId = intent.getStringExtra("NOTE_ID")

        // Load note details if editing existing note
        noteId?.let { loadNoteDetails(it) }
    }

    private fun initializeViews() {
        ivBack = findViewById(R.id.ivBack)
        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteDescription = findViewById(R.id.etNoteDescription)

        ivBack.setOnClickListener {
            saveNote()
            finish()
        }
    }

    private fun loadNoteDetails(noteId: String) {
        // First check if note is already in memory
        var note = notesDataBridge.getNoteById(noteId)

        if (note != null) {
            // If found in memory, use it
            etNoteTitle.setText(note.title)
            etNoteDescription.setText(note.description)
        } else {
            // Otherwise fetch it directly from Firestore using repository
            notesDataBridge.fetchNoteById(noteId) { fetchedNote ->
                etNoteTitle.setText(fetchedNote.title)
                etNoteDescription.setText(fetchedNote.description)
            }
        }
    }


    private fun saveNote() {
        val title = etNoteTitle.text.toString().trim()
        val description = etNoteDescription.text.toString().trim()

        Log.d("NoteEditActivity", "Saving note - Title: $title, Description: $description")

        // Only save if there's content
        if (title.isNotEmpty() || description.isNotEmpty()) {
            if (noteId != null) {
                // Update existing note
                Log.d("NoteEditActivity", "Updating existing note: $noteId")
                notesDataBridge.updateNote(noteId!!, title, description)
            } else {
                // Add new note
                Log.d("NoteEditActivity", "Adding new note")
                notesDataBridge.addNewNote(title, description)
            }
        } else {
            Log.d("NoteEditActivity", "No content to save")
        }
    }
}