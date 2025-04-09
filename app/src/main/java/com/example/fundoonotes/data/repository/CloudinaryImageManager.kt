package com.example.fundoonotes.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CloudinaryImageManager(private val context: Context) {
    private var uploadCallback: ((String?) -> Unit)? = null
    private var isInitialized = false

    init {
        initializeCloudinary()
    }

    private fun initializeCloudinary() {
        try {
            // Just initialize - no need to check if already initialized
            val config = hashMapOf(
                "cloud_name" to "dmn9c0m0u",
                "api_key" to "682551899794755",
                "api_secret" to "ZLCZWh8gJ1O7ZTiXxpOJjYKFv1k"
            )
            MediaManager.init(context, config)
            isInitialized = true
            Log.d(TAG, "Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary init failed: ${e.message}")
            isInitialized = false
        }
    }

    fun uploadProfileImage(imageUri: Uri, callback: (String?) -> Unit) {
        uploadCallback = callback

        // Make sure Cloudinary is initialized before upload
        if (!isInitialized) {
            try {
                initializeCloudinary()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize on upload attempt: ${e.message}")
                callback(null)
                Toast.makeText(context, "Failed to initialize Cloudinary service", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!isInitialized) {
            Log.e(TAG, "Cannot upload image - Cloudinary not initialized")
            callback(null)
            Toast.makeText(context, "Service not available at the moment", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Generate a unique filename to prevent overwriting
            val fileName = "profile_${UUID.randomUUID()}"

            // Start the upload process
            MediaManager.get().upload(imageUri)
                .option("public_id", fileName)
                .option("folder", "fundoo_notes/profile_images")
                .callback(object : UploadCallback {
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
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.e(TAG, "Upload error: ${error?.description}")
                        uploadCallback?.invoke(null)
                        Toast.makeText(context, "Error uploading profile picture: ${error?.description}", Toast.LENGTH_SHORT).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        Log.d(TAG, "Upload rescheduled: ${error?.description}")
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            Log.e(TAG, "Error in upload process: ${e.message}")
            uploadCallback?.invoke(null)
            Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun uploadBitmap(bitmap: Bitmap, callback: (String?) -> Unit) {
        uploadCallback = callback

        // Make sure Cloudinary is initialized before upload
        if (!isInitialized) {
            try {
                initializeCloudinary()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize on upload attempt: ${e.message}")
                callback(null)
                Toast.makeText(context, "Failed to initialize Cloudinary service", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!isInitialized) {
            Log.e(TAG, "Cannot upload image - Cloudinary not initialized")
            callback(null)
            Toast.makeText(context, "Service not available at the moment", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Convert bitmap to file
            val file = bitmapToFile(bitmap)

            if (file != null) {
                // Generate a unique filename
                val fileName = "profile_${UUID.randomUUID()}"

                // Upload the file
                MediaManager.get().upload(Uri.fromFile(file))
                    .option("public_id", fileName)
                    .option("folder", "fundoo_notes/profile_images")
                    .callback(object : UploadCallback {
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

                            // Delete the temporary file
                            file.delete()
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            Log.e(TAG, "Upload error: ${error?.description}")
                            uploadCallback?.invoke(null)
                            Toast.makeText(context, "Error uploading profile picture: ${error?.description}", Toast.LENGTH_SHORT).show()

                            // Delete the temporary file
                            file.delete()
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                            Log.d(TAG, "Upload rescheduled: ${error?.description}")
                        }
                    })
                    .dispatch()
            } else {
                Log.e(TAG, "Error: Could not convert bitmap to file")
                uploadCallback?.invoke(null)
                Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in bitmap upload process: ${e.message}")
            uploadCallback?.invoke(null)
            Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bitmapToFile(bitmap: Bitmap): File? {
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

    companion object {
        private const val TAG = "CloudinaryImageManager"
    }
}