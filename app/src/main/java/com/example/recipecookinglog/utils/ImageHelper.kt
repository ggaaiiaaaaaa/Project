package com.example.recipecookinglog.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageHelper {

    fun getImageUri(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val path = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            "Recipe_${System.currentTimeMillis()}",
            null
        )
        return Uri.parse(path)
    }

    fun compressImage(context: Context, uri: Uri): Bitmap? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val maxSize = 1024

            val ratio = Math.min(
                maxSize.toFloat() / bitmap.width,
                maxSize.toFloat() / bitmap.height
            )

            val width = (ratio * bitmap.width).toInt()
            val height = (ratio * bitmap.height).toInt()

            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } catch (e: Exception) {
            null
        }
    }
}
