package com.example.fundoonotes.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fundoonotes.R

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReminderWorker"
    }

    override suspend fun doWork(): Result {
        val noteId = inputData.getString("noteId") ?: "unknown"
        val noteTitle = inputData.getString("noteTitle") ?: "Reminder"
        val noteContent = inputData.getString("noteContent") ?: "You have a note reminder"

        Log.d(TAG, "Showing notification for note: $noteTitle (ID: $noteId)")

        showNotification(noteId, noteTitle, noteContent)

        return Result.success()
    }

    private fun showNotification(noteId: String, noteTitle: String, noteContent: String) {
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for note reminders"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(noteTitle)
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        // Use a consistent notification ID for the same note
        val notificationId = noteId.hashCode()
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notification shown with ID: $notificationId")
    }
}