package dev.arpan.calling

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build

internal object FakeCallRinger {
    private var ringtone: Ringtone? = null
    private var answeredVoicePlayer: MediaPlayer? = null
    private var answeredUseSpeaker: Boolean = false

    /** Incoming call: always the device default ringtone (never the configured caller voice). */
    fun start(context: Context) {
        answeredUseSpeaker = false
        applyIdleIncomingAudioRoute(context.applicationContext)
        stopIncomingRing()
        startDefaultRingtone(context.applicationContext)
    }

    /** After the user answers: play the custom caller voice on loop if one is set; otherwise silent. */
    fun startAnsweredVoiceIfConfigured(context: Context) {
        releaseAnsweredVoice()
        val app = context.applicationContext
        if (!CallerVoiceStore.hasCustomVoice(app)) return
        val path = CallerVoiceStore.absolutePath(app)
        applyInCallSpeakerRoute(app, answeredUseSpeaker)
        answeredVoicePlayer =
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setDataSource(path)
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrNull()
    }

    /**
     * Toggles loudspeaker vs earpiece for the active (answered) call.
     *
     * Only updates [AudioManager] ([AudioManager.MODE_IN_COMMUNICATION] and
     * [AudioManager.isSpeakerphoneOn]); the [MediaPlayer] is not stopped, released, or reconfigured
     * here so playback position is unchanged.
     */
    fun setAnsweredSpeakerphoneEnabled(context: Context, enabled: Boolean) {
        if (answeredUseSpeaker == enabled) return
        answeredUseSpeaker = enabled
        applyInCallSpeakerRoute(context.applicationContext, enabled)
    }

    /**
     * In-call routing: communication mode + speakerphone flag. Output toggles between earpiece
     * and built-in speaker without affecting the player.
     */
    private fun applyInCallSpeakerRoute(context: Context, speaker: Boolean) {
        val am = context.getSystemService(AudioManager::class.java) ?: return

        am.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val targetDevice = am.availableCommunicationDevices.firstOrNull {
                if (speaker) {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                } else {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }
            }

            if (targetDevice != null) {
                am.setCommunicationDevice(targetDevice)
            }
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = speaker
        }
    }

    /** Before / during incoming ring — do not force in-call mode. */
    private fun applyIdleIncomingAudioRoute(context: Context) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.clearCommunicationDevice()
        }
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = false
    }

    fun stopIncomingRing() {
        ringtone?.stop()
        ringtone = null
    }

    private fun releaseAnsweredVoice() {
        answeredVoicePlayer?.run {
            runCatching { stop() }
            runCatching { release() }
        }
        answeredVoicePlayer = null
    }

    fun stop(appContext: Context) {
        val ctx = appContext.applicationContext
        stopIncomingRing()
        releaseAnsweredVoice()
        answeredUseSpeaker = false
        restoreAudioRouteAfterCall(ctx)
    }

    private fun restoreAudioRouteAfterCall(context: Context) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        am.mode = AudioManager.MODE_NORMAL
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.clearCommunicationDevice()
        }
    }

    private fun startDefaultRingtone(app: Context) {
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
                isLooping = true
            }
                ?: return

        ringtone = next
        runCatching { next.play() }
    }
}
