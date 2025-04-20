package com.example.fundoonotes.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.fundoonotes.BuildConfig
import com.example.fundoonotes.data.repository.interfaces.CloudinaryImageManagerInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CloudinaryImageManager(private val context: Context): CloudinaryImageManagerInterface {

    // ==============================================
    // Properties and Initialization
    // ==============================================
    private var uploadCallback: ((String?) -> Unit)? = null
    private var isInitialized = false

    init {
        initializeCloudinary()
    }

    companion object {
        private const val TAG = "CloudinaryImageManager"
    }

    // ==============================================
    // Cloudinary Setup Methods
    // ==============================================
    override fun initializeCloudinary() {
        try {
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
            val apiKey = BuildConfig.CLOUDINARY_API_KEY
            val apiSecret = BuildConfig.CLOUDINARY_API_SECRET

            val config = HashMap<String, String>()
            config["cloud_name"] = cloudName
            config["api_key"] = apiKey
            config["api_secret"] = apiSecret
            MediaManager.init(context, config)
            isInitialized = true
            Log.d(TAG, "Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary init failed: ${e.message}")
            isInitialized = false
        }
    }

    // ==============================================
    // Image Upload Methods
    // ==============================================
    override fun uploadProfileImage(imageUri: Uri, callback: (String?) -> Unit) {
        uploadCallback = callback
        if (!ensureCloudinaryInitialized()) return

        try {
            val fileName = "profile_${UUID.randomUUID()}"

            MediaManager.get().upload(imageUri)
                .option("public_id", fileName)
                .option("folder", "fundoo_notes/profile_images")
                .callback(createUploadCallback())
                .dispatch()
        } catch (e: Exception) {
            handleUploadError(e)
        }
    }

    override fun uploadBitmap(bitmap: Bitmap, callback: (String?) -> Unit) {
        uploadCallback = callback
        if (!ensureCloudinaryInitialized()) return

        try {
            val file = bitmapToFile(bitmap)
            if (file != null) {
                val fileName = "profile_${UUID.randomUUID()}"

                MediaManager.get().upload(Uri.fromFile(file))
                    .option("public_id", fileName)
                    .option("folder", "fundoo_notes/profile_images")
                    .callback(createUploadCallback(file))
                    .dispatch()
            } else {
                handleUploadError(Exception("Could not convert bitmap to file"))
            }
        } catch (e: Exception) {
            handleUploadError(e)
        }
    }

    // ==============================================
    // Helper Methods
    // ==============================================
    private fun ensureCloudinaryInitialized(): Boolean {
        if (!isInitialized) {
            try {
                initializeCloudinary()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize on upload attempt: ${e.message}")
                uploadCallback?.invoke(null)
                Toast.makeText(context, "Failed to initialize Cloudinary service", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        if (!isInitialized) {
            Log.e(TAG, "Cannot upload image - Cloudinary not initialized")
            uploadCallback?.invoke(null)
            Toast.makeText(context, "Service not available at the moment", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createUploadCallback(file: File? = null): UploadCallback {
        return object : UploadCallback {
            override fun onStart(requestId: String?) {
                Log.d(TAG, "Upload started: $requestId")
                Toast.makeText(context, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
            }

            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                val progress = (bytes * 100) / totalBytes
                Log.d(TAG, "Upload progress: $progress%")
            }

            override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                val imageUrl = resultData?.get("secure_url") as String?
                Log.d(TAG, "Upload successful. URL: $imageUrl")
                uploadCallback?.invoke(imageUrl)
                Toast.makeText(context, "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show()
                file?.delete()
            }

            override fun onError(requestId: String?, error: ErrorInfo?) {
                Log.e(TAG, "Upload error: ${error?.description}")
                uploadCallback?.invoke(null)
                Toast.makeText(context, "Error uploading profile picture: ${error?.description}", Toast.LENGTH_SHORT).show()
                file?.delete()
            }

            override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                Log.d(TAG, "Upload rescheduled: ${error?.description}")
            }
        }
    }

    private fun handleUploadError(e: Exception) {
        Log.e(TAG, "Error in upload process: ${e.message}")
        uploadCallback?.invoke(null)
        Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    // ==============================================
    // File Conversion Methods
    // ==============================================
    override fun bitmapToFile(bitmap: Bitmap): File? {
        return try {
            val file = File(context.cacheDir, "temp_profile_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to file: ${e.message}")
            null
        }
    }
}