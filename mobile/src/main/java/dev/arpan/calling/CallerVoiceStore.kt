package dev.arpan.calling

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Persists a user-picked audio clip played on loop **after the user answers** the fake call
 * (incoming ring always uses the device default ringtone).
 */
object CallerVoiceStore {
    private const val FILE_NAME = "caller_voice_custom"

    private fun storedFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun hasCustomVoice(context: Context): Boolean {
        val f = storedFile(context)
        return f.isFile && f.length() > 0L
    }

    fun absolutePath(context: Context): String = storedFile(context).absolutePath

    fun saveFromUri(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(storedFile(context)).use { output ->
                    input.copyTo(output)
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
}
