package dev.arpan.calling

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import java.io.File
import java.io.FileOutputStream

/**
 * Persists a user-selected gallery image for the fake incoming / in-call avatar.
 * Stored under app-internal files so it survives reboots without persistable URI grants.
 */
object CallerAvatarStore {
    private const val FILE_NAME = "caller_avatar_custom.jpg"
    private const val MAX_BITMAP_SIDE_PX = 1024

    enum class AvatarStyle {
        MAIN_PREVIEW,
        INCOMING_SCREEN,
        ACTIVE_SCREEN,
    }

    private fun storedFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun hasCustomAvatar(context: Context): Boolean {
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
                        if (!decoded.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
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

    /** Full-bleed backdrop for Pixel-style “contact photo” call screen. */
    fun decodeStoredAvatarBitmap(context: Context): Bitmap? {
        if (!hasCustomAvatar(context)) return null
        return BitmapFactory.decodeFile(storedFile(context).absolutePath)
    }

    fun apply(context: Context, imageView: ImageView, style: AvatarStyle) {
        val placeholderPaddingPx =
            when (style) {
                AvatarStyle.MAIN_PREVIEW ->
                    context.resources.getDimensionPixelSize(R.dimen.main_caller_preview_padding)
                AvatarStyle.INCOMING_SCREEN,
                AvatarStyle.ACTIVE_SCREEN ->
                    context.resources.getDimensionPixelSize(R.dimen.call_avatar_padding)
            }
        val placeholderTint =
            when (style) {
                AvatarStyle.MAIN_PREVIEW ->
                    AppCompatResources.getColorStateList(context, R.color.calling_on_surface_variant)
                AvatarStyle.INCOMING_SCREEN ->
                    AppCompatResources.getColorStateList(context, R.color.call_text_secondary)
                AvatarStyle.ACTIVE_SCREEN ->
                    ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            }

        if (hasCustomAvatar(context)) {
            val path = storedFile(context).absolutePath
            val bmp = BitmapFactory.decodeFile(path)
            if (bmp != null) {
                imageView.setImageBitmap(bmp)
                imageView.imageTintList = null
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setPadding(0, 0, 0, 0)
                return
            }
        }

        imageView.setImageResource(R.drawable.ic_person_outline)
        imageView.imageTintList = placeholderTint
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setPadding(
            placeholderPaddingPx,
            placeholderPaddingPx,
            placeholderPaddingPx,
            placeholderPaddingPx,
        )
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
