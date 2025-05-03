package com.example.fundoonotes.core

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionManager(private val context: Context) {

    // ==============================================
    // Companion Object (Constants)
    // ==============================================
    companion object {
        const val NOTIFICATION_PERMISSION_CODE = 100
        const val STORAGE_PERMISSION_CODE = 101
        private const val TAG = "PermissionManager"
        private val isAndroid13OrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    // ==============================================
    // Permission Checking Methods
    // ==============================================
    fun checkNotificationPermission(activity: Activity) {
        if (isAndroid13OrHigher) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                Log.d(TAG, "Notification permission already granted")
            }
        }
    }

    fun checkScheduleExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!canScheduleExactAlarms()) {
                // Request permission via settings since this requires a special intent
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                activity.startActivity(intent)
            }
        }
    }

    fun checkStoragePermission(activity: Activity): Boolean {
        return if (isAndroid13OrHigher) {
            checkPermission(
                activity,
                Manifest.permission.READ_MEDIA_IMAGES,
                STORAGE_PERMISSION_CODE,
                "Storage"
            )
        } else {
            checkPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                STORAGE_PERMISSION_CODE,
                "Storage"
            )
        }
    }

    fun checkReminderPermissions(activity: Activity) {
        checkNotificationPermission(activity)
        checkScheduleExactAlarmPermission(activity)
    }

    // ==============================================
    // Permission Status Checking Methods
    // ==============================================
    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true  // Always true for Android versions below S
    }

    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    // ==============================================
    // Permission Request Handling
    // ==============================================
    private fun checkPermission(
        activity: Activity,
        permission: String,
        requestCode: Int,
        permissionName: String
    ): Boolean {
        return when {
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission
            ) -> {
                // Show rationale dialog
                explainPermissionRationale(activity, permissionName, permission, requestCode)
                false
            }
            else -> {
                // Request the permission
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permission),
                    requestCode
                )
                false
            }
        }
    }

    fun handlePermissionResult(
        activity: Activity,
        requestCode: Int,
        grantResults: IntArray,
        onPermissionGranted: (Int) -> Unit
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission granted: $requestCode")
            onPermissionGranted(requestCode)
        } else {
            // Check if permission was permanently denied
            when (requestCode) {
                STORAGE_PERMISSION_CODE -> {
                    val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    if (isPermissionPermanentlyDenied(activity, permissionToCheck)) {
                        showSettingsDialog(activity, "Storage")
                    }
                }
                NOTIFICATION_PERMISSION_CODE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (isPermissionPermanentlyDenied(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                            showSettingsDialog(activity, "Notification")
                        }
                    }
                }
            }
            Log.d(TAG, "Permission denied: $requestCode")
        }
    }

    // ==============================================
    // Dialog Helpers
    // ==============================================
    private fun explainPermissionRationale(
        activity: Activity,
        permissionType: String,
        permission: String,
        requestCode: Int
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("$permissionType Permission Required")
            .setMessage("This app needs $permissionType access for proper functionality.")
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permission),
                    requestCode
                )
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Log.d(TAG, "$permissionType permission denied by user after explanation")
            }
            .show()
    }

    fun showSettingsDialog(activity: Activity, permissionType: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("$permissionType Permission Required")
            .setMessage("$permissionType permission is needed but has been permanently denied. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", activity.packageName, null)
                intent.data = uri
                activity.startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}