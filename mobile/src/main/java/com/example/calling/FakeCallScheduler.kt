package com.example.calling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

internal object FakeCallScheduler {
    private const val REQUEST_CODE: Int = 71011
    private const val PREFS: String = "fake_call_scheduler"
    private const val KEY_CALLER: String = "caller"
    private const val KEY_TRIGGER_AT: String = "trigger_at"

    fun dispatch(context: Context, request: FakeCallRequest) {
        val caller = sanitizeCaller(context, request.callerName)
        val normalized = request.copy(callerName = caller, delaySeconds = request.delaySeconds.coerceAtLeast(0))
        if (normalized.delaySeconds <= 0) {
            clearScheduledState(context)
            FakeCallNotifier.show(context, normalized.callerName)
            return
        }
        schedule(context, normalized)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context))
        pendingIntent(context).cancel()
        clearScheduledState(context)
    }

    fun describeScheduledCall(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val caller = prefs.getString(KEY_CALLER, null) ?: return null
        val triggerAt = prefs.getLong(KEY_TRIGGER_AT, 0L)
        if (triggerAt <= System.currentTimeMillis()) {
            clearScheduledState(context)
            return null
        }
        return context.getString(
            R.string.phone_scheduled_summary,
            caller,
            android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(triggerAt)),
        )
    }

    private fun schedule(context: Context, request: FakeCallRequest) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = System.currentTimeMillis() + (request.delaySeconds * 1000L)
        persistScheduledState(context, request.callerName, triggerAt)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context, request))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context, request))
        }
    }

    private fun pendingIntent(
        context: Context,
        request: FakeCallRequest? = null,
    ): PendingIntent {
        val intent =
            Intent(context, FakeCallAlarmReceiver::class.java).apply {
                if (request != null) {
                    putExtra(FakeIncomingCallActivity.EXTRA_CALLER, request.callerName)
                }
            }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun sanitizeCaller(context: Context, callerName: String): String {
        return callerName.trim().ifBlank { context.getString(R.string.fake_call_default_name) }
    }

    private fun persistScheduledState(context: Context, caller: String, triggerAt: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CALLER, caller)
            .putLong(KEY_TRIGGER_AT, triggerAt)
            .apply()
    }

    internal fun clearScheduledState(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CALLER)
            .remove(KEY_TRIGGER_AT)
            .apply()
    }
}
