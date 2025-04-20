package com.example.fundoonotes.ui.noteEdit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fundoonotes.R
import com.example.fundoonotes.core.PermissionManager
import com.example.fundoonotes.data.model.Label
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class NoteEditActivity : AppCompatActivity() {

    // ==============================================
    // ViewModel and Dependencies
    // ==============================================
    private lateinit var viewModel: NoteEditViewModel
    private lateinit var permissionManager: PermissionManager

    // ==============================================
    // UI Components
    // ==============================================
    private lateinit var ivBack: ImageView
    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteDescription: EditText
    private lateinit var ivReminder: ImageView
    private lateinit var ivArchive: ImageView
    private lateinit var labelChipGroup: ChipGroup

    // ==============================================
    // Companion Object
    // ==============================================
    companion object { private const val TAG = "NoteEditActivity" }

    // ==============================================
    // Activity Lifecycle Methods
    // ==============================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_note_edit)

        viewModel = ViewModelProvider(this)[NoteEditViewModel::class.java]
        permissionManager = PermissionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupObservers()

        // Load note data
        val noteId = intent.getStringExtra("NOTE_ID")
        viewModel.loadNote(noteId)
    }

    // ==============================================
    // View Initialization
    // ==============================================
    private fun initializeViews() {
        ivBack = findViewById(R.id.ivBack)
        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteDescription = findViewById(R.id.etNoteDescription)
        ivReminder = findViewById(R.id.ivReminder)
        labelChipGroup = findViewById(R.id.label_chip_group)
        ivArchive = findViewById(R.id.ivArchive)

        ivArchive.setOnClickListener {
            viewModel.toggleArchive()
        }

        ivReminder.setOnClickListener {
            setupReminderPicker()
        }

        ivBack.setOnClickListener {
            viewModel.saveNote(
                etNoteTitle.text.toString().trim(),
                etNoteDescription.text.toString().trim()
            )
        }
    }

    // ==============================================
    // ViewModel Observers
    // ==============================================
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentNote.collect { note ->
                        note?.let {
                            etNoteTitle.setText(it.title)
                            etNoteDescription.setText(it.description)
                        }
                    }
                }

                launch {
                    viewModel.noteLabels.collect { labelIds ->
                        populateLabelChips(labelIds)
                    }
                }

                launch {
                    viewModel.success.collect { success ->
                        if (success) {
                            finish()
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(this@NoteEditActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                launch {
                    viewModel.createNoteArchived.collect { archived ->
                        if (archived) {
                            Toast.makeText(this@NoteEditActivity, "Note will be archived once you save it", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    viewModel.reminderTime.collect { time ->
                        time?.let {
                            // Update UI to show reminder is set
                            ivReminder.setImageResource(R.drawable.alarm) // Use your own icon
                            // Optionally show the date/time somewhere
                        } ?: run {
                            // No reminder set
                            ivReminder.setImageResource(R.drawable.alarm)
                        ivReminder.setColorFilter(R.color.black)// Default icon
                        }
                    }
                }
            }
        }
    }

    // ==============================================
    // Label Management
    // ==============================================
    private fun populateLabelChips(labelIds: List<String>) {
        labelChipGroup.removeAllViews()

        viewModel.availableLabels.value?.forEach { label ->
            if (labelIds.contains(label.id)) {
                addLabelChip(label, true)
            }
        }

        // Add the "Add Label" chip
        val addLabelChip = Chip(this)
        addLabelChip.text = "+ Add Label"
        addLabelChip.isCheckable = false
        addLabelChip.setOnClickListener {
            showLabelSelectionDialog()
        }
        labelChipGroup.addView(addLabelChip)
    }

    private fun addLabelChip(label: Label, isSelected: Boolean) {
        val chip = Chip(this)
        chip.text = label.name
        chip.isCloseIconVisible = true
        chip.isChecked = isSelected
        chip.setOnCloseIconClickListener {
            viewModel.removeLabel(label.id)
        }
        labelChipGroup.addView(chip, labelChipGroup.childCount - 1) // Add before the "Add Label" chip
    }

    private fun showLabelSelectionDialog() {
        val availableLabels = viewModel.getAvailableLabelsForDialog()

        if (availableLabels.isEmpty()) {
            Toast.makeText(this, "No more labels available. Create new labels in the Labels section.", Toast.LENGTH_LONG).show()
            return
        }

        val labelNames = availableLabels.map { it.name }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Label")
            .setItems(labelNames) { _, which ->
                viewModel.addLabel(availableLabels[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==============================================
    // Reminder Management
    // ==============================================
    private fun setupReminderPicker() {
        // Check reminder permissions first
        permissionManager.checkReminderPermissions(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Check if a reminder is already set
        val currentReminderTime = viewModel.reminderTime.value
        if (currentReminderTime != null) {
            // Show confirmation dialog to cancel the existing reminder
            showCancelReminderDialog(currentReminderTime)
        } else {
            // No reminder set, proceed with setting a new one
            showDateTimePicker()
        }
    }

    private fun showCancelReminderDialog(reminderTime: Long) {
        val formattedDateTime = SimpleDateFormat("MMM dd, yyyy HH:mm").format(reminderTime)

        val materialDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Reminder")
            .setMessage("Do you want to cancel the reminder set for $formattedDateTime?")
            .setPositiveButton("Yes") { _, _ ->
                // Cancel the reminder
                viewModel.cancelReminder()
                Log.d(TAG, "Reminder cancelled")
                Toast.makeText(this, "Reminder cancelled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No") { _, _ ->
                showDateTimePicker()
            }
            .create()

        materialDialog.show()
    }

    private fun showDateTimePicker() {
        val currentDate = Calendar.getInstance()

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            currentDate.set(Calendar.YEAR, year)
            currentDate.set(Calendar.MONTH, month)
            currentDate.set(Calendar.DAY_OF_MONTH, day)

            val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                currentDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                currentDate.set(Calendar.MINUTE, minute)

                viewModel.setReminderTime(currentDate.timeInMillis)

                // Show toast with formatted time
                val formattedDateTime = SimpleDateFormat("MMM dd, yyyy HH:mm").format(currentDate.timeInMillis)

                if (formattedDateTime > SimpleDateFormat("MMM dd, yyyy HH:mm").format(System.currentTimeMillis())){
                    Toast.makeText(this, "Reminder set for: $formattedDateTime", Toast.LENGTH_SHORT).show()
                }

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

    // ==============================================
    // Permission Handling
    // ==============================================
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
                Log.d(TAG, "Notification permission granted, can proceed with reminder")
                showDateTimePicker()
            }
        }
    }
}