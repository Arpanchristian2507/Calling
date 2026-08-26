package dev.arpan.calling

import android.app.Activity
import android.app.NotificationChannel
import android.app.PendingIntent
import androidx.core.app.NotificationCompat

internal fun NotificationCompat.Builder.applyPersonalIncomingNotification(
    pendingIntent: PendingIntent,
): NotificationCompat.Builder = this

internal fun NotificationChannel.applyPersonalIncomingChannel() = Unit

internal fun Activity.applyPersonalIncomingWindowFlags() = Unit
