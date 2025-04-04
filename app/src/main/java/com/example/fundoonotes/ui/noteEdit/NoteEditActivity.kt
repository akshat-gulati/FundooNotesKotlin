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
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.data.repository.ReminderScheduler
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NoteEditActivity : AppCompatActivity(){
    private lateinit var ivBack: ImageView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
    private lateinit var ivReminder: ImageView

    private var noteLabels: ArrayList<String> = ArrayList()
    private lateinit var labelChipGroup: ChipGroup

    //    Added a class variable to store the reminder time temporarily
    private var reminderTime: Long? = null
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TAG = "NoteEditActivity"
    }



    //    private lateinit var firestoreNoteRepository: FirestoreNoteRepository
    private lateinit var notesDataBridge: NotesDataBridge
    private lateinit var labelDataBridge: LabelDataBridge

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
        labelDataBridge = LabelDataBridge(applicationContext)

        initializeViews()

        // Get note ID from intent
        noteId = intent.getStringExtra("NOTE_ID")

// Load note details if editing existing note
        if (noteId != null) {
            loadNoteDetails(noteId!!)
        } else {
            val addLabelChip = Chip(this)
            addLabelChip.text = "+ Add Label"
            addLabelChip.isCheckable = false
            addLabelChip.setOnClickListener {
                showLabelSelectionDialog()
            }
            labelChipGroup.addView(addLabelChip)
        }

        checkNotificationPermission()
        labelDataBridge.fetchLabels()
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
        labelChipGroup = findViewById(R.id.label_chip_group)

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
            noteLabels.clear()
            noteLabels.addAll(note.labels) // Add the note's label IDs
            loadLabels() // Call loadLabels() after setting noteLabels
        } else {
            // Otherwise fetch it directly from Firestore using repository
            notesDataBridge.fetchNoteById(noteId) { fetchedNote ->
                etNoteTitle.setText(fetchedNote.title)
                etNoteDescription.setText(fetchedNote.description)
                noteLabels.clear()
                noteLabels.addAll(fetchedNote.labels) // Add the fetched note's label IDs
                loadLabels() // Call loadLabels() after setting noteLabels
            }
        }
    }


    private fun loadLabels() {
        lifecycleScope.launch {
            try {
                val allLabels = labelDataBridge.labelsState.first()

                // Clear existing chips
                labelChipGroup.removeAllViews()

                // Add chips for existing labels on this note
                noteLabels.forEach { labelId ->
                    val label = allLabels.find { it.id == labelId }
                    label?.let {
                        addLabelChip(it, true)
                    }
                }

                // Add a special chip to add new labels
                val addLabelChip = Chip(this@NoteEditActivity)
                addLabelChip.text = "+ Add Label"
                addLabelChip.isCheckable = false
                addLabelChip.setOnClickListener {
                    showLabelSelectionDialog()
                }
                labelChipGroup.addView(addLabelChip)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading labels", e)
                Toast.makeText(this@NoteEditActivity, "Error loading labels", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addLabelChip(label: Label, isSelected: Boolean) {
        val chip = Chip(this)
        chip.text = label.name
        chip.isCloseIconVisible = true
        chip.isChecked = isSelected
        chip.setOnCloseIconClickListener {
            // Remove label from note
            noteLabels.remove(label.id)
            labelChipGroup.removeView(chip)

            // Update the note if it exists
            noteId?.let { id ->
                removeNoteFromLabel(id, label.id)
            }
        }
        labelChipGroup.addView(chip, labelChipGroup.childCount) // Add before the "Add Label" chip
    }

    private fun removeNoteFromLabel(noteId: String, labelId: String) {
        // Get the current label
        labelDataBridge.fetchLabelById(labelId) { label ->
            // Create new noteIds list without this note
            val updatedNoteIds = label.noteIds.filter { it != noteId }
            // Update the label
            labelDataBridge.updateLabel(labelId, label.name, updatedNoteIds)
        }
    }
    private fun addNoteToLabel(noteId: String, labelId: String) {
        // Get the current label
        labelDataBridge.fetchLabelById(labelId) { label ->
            // Create new noteIds list with this note (if not already there)
            val updatedNoteIds = if (!label.noteIds.contains(noteId)) {
                label.noteIds + noteId
            } else {
                label.noteIds
            }
            // Update the label
            labelDataBridge.updateLabel(labelId, label.name, updatedNoteIds)
        }
    }

    private fun showLabelSelectionDialog() {
        lifecycleScope.launch {
            try {
                val allLabels = labelDataBridge.labelsState.first()
                val availableLabels = allLabels.filter { it.id !in noteLabels }

                if (availableLabels.isEmpty()) {
                    Toast.makeText(this@NoteEditActivity, "No more labels available. Create new labels in the Labels section.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val labelNames = availableLabels.map { it.name }.toTypedArray()

                androidx.appcompat.app.AlertDialog.Builder(this@NoteEditActivity)
                    .setTitle("Add Label")
                    .setItems(labelNames) { _, which ->
                        val selectedLabel = availableLabels[which]
                        if (!noteLabels.contains(selectedLabel.id)) {
                            noteLabels.add(selectedLabel.id)
                        }
                        addLabelChip(selectedLabel, true)

                        // If the note already exists, update the relationship
                        noteId?.let { id ->
                            addNoteToLabel(id, selectedLabel.id)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing label selection", e)
                Toast.makeText(this@NoteEditActivity, "Error loading labels", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_note_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_save -> {
                saveNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                // Include noteLabels when updating
                notesDataBridge.updateNoteWithLabels(noteId!!, title, description, reminderTime, noteLabels)

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
                // Include noteLabels when adding
                val newNoteId = notesDataBridge.addNewNoteWithLabels(title, description, reminderTime, noteLabels)
                Log.d("NoteEditActivity", "New note ID: $newNoteId")

                // Schedule reminder for new note
                reminderTime?.let { time ->
                    // Create a temporary Note object with the basic info we have
                    val tempNote = Note(
                        id = newNoteId,
                        title = title,
                        description = description,
                        reminderTime = time,
                        labels = noteLabels
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


