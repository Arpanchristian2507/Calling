package dev.arpan.calling

import android.app.Activity
import android.app.NotificationChannel
import android.app.PendingIntent
import androidx.core.app.NotificationCompat

internal fun NotificationCompat.Builder.applyPersonalIncomingNotification(
    pendingIntent: PendingIntent,
): NotificationCompat.Builder =
    setCategory(NotificationCompat.CATEGORY_CALL)
        .setFullScreenIntent(pendingIntent, true)

internal fun NotificationChannel.applyPersonalIncomingChannel() {
    setBypassDnd(true)
}

internal fun Activity.applyPersonalIncomingWindowFlags() {
    setShowWhenLocked(true)
    setTurnScreenOn(true)
}
