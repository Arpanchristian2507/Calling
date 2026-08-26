package dev.arpan.calling

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Full-screen call background image (Samsung-style custom gallery), stored on device.
 * Separate from [CallerAvatarStore], which powers the circular caller photo.
 */
object CallBackgroundImageStore {
    private const val FILE_NAME = "call_screen_full_background.jpg"
    private const val MAX_BITMAP_SIDE_PX = 2560

    private fun storedFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun hasCustomBackground(context: Context): Boolean {
        val f = storedFile(context)
        return f.isFile && f.length() > 0L
    }

    fun saveFromGalleryUri(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                val decoded = decodeSampledBitmap(bytes) ?: return@use false
                try {
                    FileOutputStream(storedFile(context)).use { out ->
                        if (!decoded.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                            return@use false
                        }
                    }
                } finally {
                    decoded.recycle()
                }
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun clear(context: Context) {
        storedFile(context).delete()
    }

    fun decodeIfPresent(context: Context): Bitmap? {
        if (!hasCustomBackground(context)) return null
        return BitmapFactory.decodeFile(storedFile(context).absolutePath)
    }

    private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var inSampleSize = 1
        while (bounds.outWidth / inSampleSize > MAX_BITMAP_SIDE_PX ||
            bounds.outHeight / inSampleSize > MAX_BITMAP_SIDE_PX
        ) {
            inSampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = inSampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
