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
import com.example.fundoonotes.data.repository.NoteRepository

class NoteEditActivity : AppCompatActivity() {
    private lateinit var ivBack: ImageView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
    private lateinit var noteRepository: NoteRepository
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
        noteRepository = NoteRepository(applicationContext)

        initializeViews()

        // Get note ID from intent
        noteId = intent.getStringExtra("NOTE_ID")

        // Load note details if editing existing note
        if (noteId != null) {
            loadNoteDetails(noteId!!)
        }
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
        val note = noteRepository.getNoteById(noteId)
        note?.let {
            etNoteTitle.setText(it.title)
            etNoteDescription.setText(it.description)
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
                noteRepository.updateNote(noteId!!, title, description)
            } else {
                // Add new note
                Log.d("NoteEditActivity", "Adding new note")
                noteRepository.addNewNote(title, description)
            }
        } else {
            Log.d("NoteEditActivity", "No content to save")
        }
    }
}