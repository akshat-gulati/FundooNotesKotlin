package com.example.fundoonotes.ui.noteEdit

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.data.repository.ReminderScheduler

class NoteEditActivity : AppCompatActivity(){
    private lateinit var ivBack: ImageView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
    private lateinit var ivReminder: ImageView

//    Added a class variable to store the reminder time temporarily
    private var reminderTime: Long? = null
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }



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

        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        // Check POST_NOTIFICATIONS for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d("NoteEditActivity", "Notification permission already granted")
            }
        }

        // Check SCHEDULE_EXACT_ALARM for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Request permission
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    // Handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("NoteEditActivity", "Notification permission granted")
                } else {
                    Log.d("NoteEditActivity", "Notification permission denied")
                }
            }
        }
    }

    private fun initializeViews() {
        ivBack = findViewById(R.id.ivBack)
        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteDescription = findViewById(R.id.etNoteDescription)
        ivReminder = findViewById(R.id.ivReminder)

        // Don't set the click listener here, just call the function
        ivReminder.setOnClickListener {
            setupReminderPicker()
        }

        ivBack.setOnClickListener {
            saveNote()
            finish()
        }
    }

    private fun setupReminderPicker() {
        // Remove the click listener here - it's already set in initializeViews()
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
            val reminderScheduler = ReminderScheduler(applicationContext)

            if (noteId != null) {
                // Update existing note
                Log.d("NoteEditActivity", "Updating existing note: $noteId")
                notesDataBridge.updateNote(noteId!!, title, description, reminderTime)

                // Schedule reminder for existing note
                reminderTime?.let { time ->
                    val note = notesDataBridge.getNoteById(noteId!!)
                    note?.let {
                        Log.d("NoteEditActivity", "About to schedule reminder for existing note: ${it.id} at time: $time")
                        reminderScheduler.scheduleReminder(it, time)
                    }
                }
            } else {
                // Add new note and get the new ID
                Log.d("NoteEditActivity", "Adding new note")
                val newNoteId = notesDataBridge.addNewNote(title, description, reminderTime)
                Log.d("NoteEditActivity", "New note ID: $newNoteId")

                // Schedule reminder for new note
                reminderTime?.let { time ->
                    // Create a temporary Note object with the basic info we have
                    val tempNote = Note(
                        id = newNoteId,
                        title = title,
                        description = description,
                        reminderTime = time
                    )
                    Log.d("NoteEditActivity", "About to schedule reminder for new note: $newNoteId at time: $time")
                    reminderScheduler.scheduleReminder(tempNote, time)
                }
            }
        } else {
            Log.d("NoteEditActivity", "No content to save")
        }
    }


    @SuppressLint("SimpleDateFormat")
    private fun saveReminderTime(reminderTime: Long) {
        val currentTime = System.currentTimeMillis()

        // Check if reminder time is in the future
        if (reminderTime <= currentTime) {
            Log.e("NoteEditActivity", "Cannot set reminder in the past")
            // Show error toast or message to user
            return
        }

        // Save reminder time to the note
        val formattedDateTime = SimpleDateFormat("MMM dd, yyyy HH:mm").format(reminderTime)
        Log.d("NoteEditActivity", "Reminder set for: $formattedDateTime")

        // Store temporarily until saved with the note
        this.reminderTime = reminderTime
    }

}

