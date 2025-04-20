package com.example.fundoonotes.data.repository.reminder

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fundoonotes.data.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WorkManagerReminderScheduler(private val context: Context) {

    companion object {
        private const val TAG = "WorkManagerScheduler"
    }

    fun scheduleReminder(note: Note, triggerTimeMillis: Long) {
        val currentTime = System.currentTimeMillis()

        // Verify trigger time is in the future
        if (triggerTimeMillis <= currentTime) {
            Log.e(TAG, "Cannot schedule reminder in the past: $triggerTimeMillis <= $currentTime")
            return
        }

        // Calculate delay in milliseconds
        val delayMillis = triggerTimeMillis - currentTime

        // Build input data
        val inputData = Data.Builder()
            .putString("noteId", note.id)
            .putString("noteTitle", note.title)
            .putString("noteContent", note.description)
            .build()

        // Create work request
        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("reminder_${note.id}")
            .build()

        // Schedule work with a unique name based on note ID
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "reminder_work_${note.id}",
                ExistingWorkPolicy.REPLACE,  // Replace if already exists
                reminderRequest
            )

        val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
        Log.d(TAG, "Reminder work scheduled at: ${formatted.format(Date(triggerTimeMillis))} " +
                "for note: ${note.title} (${note.id})")
    }

    fun cancelReminder(noteId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_work_$noteId")
        Log.d(TAG, "Cancelled reminder for note ID: $noteId")
    }
}