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
import com.example.fundoonotes.core.PermissionManager
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.NoteLabelRepository
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.data.repository.ReminderScheduler
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class NoteEditActivity : AppCompatActivity(){
    private lateinit var ivBack: ImageView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
    private lateinit var ivReminder: ImageView
    private lateinit var ivArchive: ImageView


    private var noteLabels: ArrayList<String> = ArrayList()
    private lateinit var labelChipGroup: ChipGroup

    //    Added a class variable to store the reminder time temporarily
    private var reminderTime: Long? = null
    companion object {
        private const val TAG = "NoteEditActivity"
    }
    //    private lateinit var firestoreNoteRepository: FirestoreNoteRepository
    private lateinit var notesDataBridge: NotesDataBridge
    private lateinit var labelDataBridge: LabelDataBridge
    private lateinit var noteLabelRepository: NoteLabelRepository
    private lateinit var permissionManager: PermissionManager

    private var noteId: String? = null
    private var createNoteArchived = false

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
        notesDataBridge = NotesDataBridge(applicationContext)
        labelDataBridge = LabelDataBridge(applicationContext)
        noteLabelRepository = NoteLabelRepository(applicationContext)
        permissionManager = PermissionManager(this)

        initializeViews()

        // Get note ID from intent
        noteId = intent.getStringExtra("NOTE_ID")

// Load note details if editing existing note
        if (noteId != null) {
            loadNoteDetails(noteId!!)
        } else {
            noteId = UUID.randomUUID().toString()

            val addLabelChip = Chip(this)
            addLabelChip.text = "+ Add Label"
            addLabelChip.isCheckable = false
            addLabelChip.setOnClickListener {
                showLabelSelectionDialog()
            }
            labelChipGroup.addView(addLabelChip)
        }

//        checkNotificationPermission()
        labelDataBridge.fetchLabels()
    }

    // Handle permission results
    private fun initializeViews() {
        ivBack = findViewById(R.id.ivBack)
        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteDescription = findViewById(R.id.etNoteDescription)
        ivReminder = findViewById(R.id.ivReminder)
        labelChipGroup = findViewById(R.id.label_chip_group)
        ivArchive = findViewById(R.id.ivArchive)

        ivArchive.setOnClickListener {
            val note = notesDataBridge.getNoteById(noteId!!)
            if (note != null){
                try {
                    notesDataBridge.toggleNoteToArchive(noteId!!)
                    Toast.makeText(this, "Toggling note to archive", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error toggling note to archive ${e}", Toast.LENGTH_LONG).show()
                }

            }
            else{
                createNoteArchived = true
                Toast.makeText(this, "Note will be archived once you save it", Toast.LENGTH_LONG).show()
            }

        }

        ivReminder.setOnClickListener {
            setupReminderPicker()
        }

        ivBack.setOnClickListener {
            saveNote()
            finish()
        }
    }

    // In NoteEditActivity.kt

    private fun setupReminderPicker() {
        // Check reminder permissions first
        permissionManager.checkReminderPermissions(this)

        // We'll continue with the date/time picker only if permissions are granted
        // This is where we need a callback mechanism

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                // We'll request in checkReminderPermissions and catch in onRequestPermissionsResult
                return
            }
        }

        // If we have permissions (or don't need them on older Android), proceed with pickers
        showDateTimePicker()
    }

    private fun showDateTimePicker() {
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

    // Update the onRequestPermissionsResult method to call showDateTimePicker when permissions are granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionManager.handlePermissionResult(
            this,
            requestCode,
            permissions,
            grantResults
        ) { code ->
            if (code == PermissionManager.NOTIFICATION_PERMISSION_CODE) {
                // Continue with reminder setup after permission granted
                Log.d(TAG, "Notification permission granted, can proceed with reminder")
                showDateTimePicker()  // Show the date/time picker now that we have permission
            }
        }
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



    private fun saveNote() {
        val title = etNoteTitle.text.toString().trim()
        val description = etNoteDescription.text.toString().trim()

        // Only save if there's content
        if (title.isNotEmpty() || description.isNotEmpty()) {
            val reminderScheduler = ReminderScheduler(applicationContext)

            // At this point noteId should never be null since we either got it from intent or generated it with UUID.randomUUID()
            val currentNoteId = noteId!! // We can safely use !! here since we ensure it's not null

            // Check if this note already exists in the database
            val existingNote = notesDataBridge.getNoteById(currentNoteId)

            if (existingNote != null) {
                // Update existing note
                Log.d("NoteEditActivity", "Updating existing note: $currentNoteId")
                noteLabelRepository.updateNoteWithLabels(currentNoteId, title, description, reminderTime, noteLabels)

                // Schedule reminder if needed
                reminderTime?.let { time ->
                    Log.d("NoteEditActivity", "Scheduling reminder for existing note at time: $time")
                    reminderScheduler.scheduleReminder(existingNote.copy(reminderTime = time), time)
                }
            } else {
                // Add new note with our pre-generated ID
                Log.d("NoteEditActivity", "Adding new note with ID: $currentNoteId")
                noteLabelRepository.addNewNoteWithLabels(currentNoteId, title, description, reminderTime, noteLabels)

                // Schedule reminder if needed
                reminderTime?.let { time ->
                    // Create a Note object with our data
                    val newNote = Note(id = currentNoteId, title = title, description = description, reminderTime = time, labels = noteLabels)
                    Log.d("NoteEditActivity", "Scheduling reminder for new note at time: $time")
                    reminderScheduler.scheduleReminder(newNote, time)
                }
                if (createNoteArchived) {
                    notesDataBridge.toggleNoteToArchive(currentNoteId)
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
            Toast.makeText(this, "Cannot set reminder in the past", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if we can schedule exact alarms
        if (!permissionManager.canScheduleExactAlarms()) {
            Log.e("NoteEditActivity", "Cannot schedule exact alarms")
            Toast.makeText(this, "Permission to schedule exact alarms is required", Toast.LENGTH_SHORT).show()
            permissionManager.checkScheduleExactAlarmPermission(this)
            return
        }

        // Save reminder time to the note
        val formattedDateTime = SimpleDateFormat("MMM dd, yyyy HH:mm").format(reminderTime)
        Log.d("NoteEditActivity", "Reminder set for: $formattedDateTime")
        Toast.makeText(this, "Reminder set for: $formattedDateTime", Toast.LENGTH_SHORT).show()

        // Store temporarily until saved with the note
        this.reminderTime = reminderTime
    }

}