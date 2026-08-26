package dev.arpan.calling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

internal object FakeCallNotifier {
    const val CHANNEL_ID: String = "calling_fake_incoming"
    private const val NOTIFICATION_ID: Int = 71001

    fun show(context: Context, callerName: String) {
        val app = context.applicationContext
        ensureChannel(app)

        val activityIntent =
            Intent(app, FakeIncomingCallActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(FakeIncomingCallActivity.EXTRA_CALLER, callerName)
            }

        // Open the call UI immediately (store-safe builds omit full-screen intent on the notification).
        app.startActivity(activityIntent)

        val pendingIntent =
            android.app.PendingIntent.getActivity(
                app,
                0,
                activityIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(app.getString(R.string.fake_call_notification_title))
                .setContentText(callerName)
                .setStyle(NotificationCompat.BigTextStyle().bigText(callerName))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .applyPersonalIncomingNotification(pendingIntent)

        NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.fake_call_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            )
        channel.applyPersonalIncomingChannel()
        channel.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(channel)
    }
}
