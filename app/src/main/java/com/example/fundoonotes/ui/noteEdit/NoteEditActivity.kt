package com.example.fundoonotes.ui.noteEdit

import android.os.Bundle
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

        initializeViews()

        noteId = intent.getStringExtra("NOTE_ID")
        noteRepository = NoteRepository()

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
        val title = etNoteTitle.text.toString()
        val description = etNoteDescription.text.toString()

        if (noteId != null) {
            // Update existing note
            noteRepository.updateNote(noteId!!, title, description)
        } else {
            // Add new note
            noteRepository.addNewNote(title, description)
        }
    }
}