package com.example.fundoonotes.data.repository


import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.example.fundoonotes.core.PermissionManager
import com.example.fundoonotes.data.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderScheduler(private val context: Context) {

    private val permissionManager = PermissionManager(context)

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleReminder(note: Note, triggerTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        // Verify trigger time is in the future
        if (triggerTimeMillis <= currentTime) {
            Log.e("ReminderScheduler", "Cannot schedule reminder in the past: ${triggerTimeMillis} <= ${currentTime}")
            return
        }

        // Check for exact alarm permission on Android S and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !permissionManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Exact alarm permission not granted", Toast.LENGTH_SHORT).show()
            requestExactAlarmPermission(context)
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteTitle", note.title)
            putExtra("noteContent", note.description)
            putExtra("noteId", note.id)
        }

        // Create unique request code based on note ID
        val requestCode = note.id.hashCode()

        Log.d("ReminderScheduler", "Creating pending intent with request code: $requestCode")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // For Android S+ we've already checked permission above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }

            val formatted = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
            Log.d("ReminderScheduler", "Reminder successfully scheduled at: ${formatted.format(Date(triggerTimeMillis))} for note: ${note.title} (${note.id})")
        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "SecurityException when scheduling reminder: ${e.message}")
        }
    }

    private fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}