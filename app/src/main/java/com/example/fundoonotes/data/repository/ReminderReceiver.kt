package com.example.fundoonotes.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fundoonotes.R
import kotlin.math.log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d("ReminderReceiver", "onReceive called")
        if (context == null || intent == null) {
            Log.e("ReminderReceiver", "Received null context or intent")
            return
        }

        val noteId = intent.getStringExtra("noteId") ?: "unknown"
        val noteTitle = intent.getStringExtra("noteTitle") ?: "Reminder"
        val noteContent = intent.getStringExtra("noteContent") ?: "You have a note reminder"

        Log.d("ReminderReceiver", "Notification triggered for note: $noteTitle (ID: $noteId)")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            Log.d("ReminderReceiver", "Created notification channel: $channelId")
        }

        val notification = NotificationCompat.Builder(context, channelId)
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
        Log.d("ReminderReceiver", "Notification shown with ID: $notificationId")
    }
}