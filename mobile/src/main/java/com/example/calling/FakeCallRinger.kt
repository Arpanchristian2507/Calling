package com.example.calling

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build

internal object FakeCallRinger {
    private var ringtone: Ringtone? = null

    fun start(context: Context) {
        stop()
        val app = context.applicationContext
        val ringtoneUri =
            RingtoneManager.getActualDefaultRingtoneUri(app, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return

        val next =
            RingtoneManager.getRingtone(app, ringtoneUri)?.apply {
                audioAttributes =
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
            }
                ?: return

        ringtone = next
        runCatching { next.play() }
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
    }
}
