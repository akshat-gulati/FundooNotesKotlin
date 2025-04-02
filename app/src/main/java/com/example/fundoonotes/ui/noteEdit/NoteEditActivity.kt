package com.example.fundoonotes.ui.noteEdit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fundoonotes.R
import com.example.fundoonotes.data.repository.NotesDataBridge

class NoteEditActivity : AppCompatActivity(){
    private lateinit var ivBack: ImageView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
    private lateinit var ivReminder: ImageView
    private lateinit var ivArchive: ImageView

//    Added a class variable to store the reminder time temporarily
    private var reminderTime: Long? = null



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
        ivReminder = findViewById(R.id.ivReminder)

        ivReminder.setOnClickListener {
            setupReminderPicker()
        }


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
                notesDataBridge.updateNote(noteId!!, title, description, reminderTime)
            } else {
                // Add new note
                Log.d("NoteEditActivity", "Adding new note")
                notesDataBridge.addNewNote(title, description, reminderTime)
            }
        } else {
            Log.d("NoteEditActivity", "No content to save")
        }
    }

    private fun setupReminderPicker() {
        ivReminder = findViewById(R.id.ivReminder)

        ivReminder.setOnClickListener {
            // First show date picker
            val currentDate = Calendar.getInstance()

            val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                // After date is picked, update calendar and show time picker
                currentDate.set(Calendar.YEAR, year)
                currentDate.set(Calendar.MONTH, month)
                currentDate.set(Calendar.DAY_OF_MONTH, day)

                // Show time picker after date is selected
                val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    currentDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    currentDate.set(Calendar.MINUTE, minute)

                    // At this point, currentDate has the complete date and time
                    saveReminderTime(currentDate.timeInMillis)
                }

                TimePickerDialog(
                    this,
                    timeListener,
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    true
                ).show()
            }

            DatePickerDialog(
                this,
                dateListener,
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun saveReminderTime(reminderTime: Long) {
        // Save reminder time to the note
        // You may want to display this somewhere in your UI too
        val formattedDateTime = SimpleDateFormat("MMM dd, yyyy HH:mm").format(reminderTime)
        Log.d("NoteEditActivity", "Reminder set for: $formattedDateTime")

        // Store temporarily until saved with the note
        this.reminderTime = reminderTime
    }

}

