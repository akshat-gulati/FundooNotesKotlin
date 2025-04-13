package com.example.fundoonotes.data.repository.interfaces

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

interface CloudinaryImageManagerInterface {

    fun initializeCloudinary()
    fun uploadProfileImage(imageUri: Uri, callback: (String?) -> Unit)
    fun uploadBitmap(bitmap: Bitmap, callback: (String?) -> Unit)
    fun bitmapToFile(bitmap: Bitmap): File?
}