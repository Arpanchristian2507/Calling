package dev.arpan.calling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

internal data class ScheduledCall(
    val id: String,
    val callerName: String,
    val triggerAtMillis: Long,
    val requestCode: Int,
)

internal object FakeCallScheduler {
    private const val PREFS: String = "fake_call_scheduler"
    private const val KEY_SCHEDULES_JSON: String = "schedules_json"
    private const val KEY_NEXT_REQUEST_CODE: String = "next_request_code"
    private const val BASE_REQUEST_CODE: Int = 71_012
    private const val LEGACY_REQUEST_CODE: Int = 71_011
    private const val LEGACY_KEY_CALLER: String = "caller"
    private const val LEGACY_KEY_TRIGGER_AT: String = "trigger_at"

    fun dispatch(context: Context, request: FakeCallRequest) {
        val caller = sanitizeCaller(context, request.callerName)
        val normalized =
            request.copy(
                callerName = caller,
                delaySeconds = request.delaySeconds.coerceAtLeast(0),
            )
        val now = System.currentTimeMillis()
        val atMillis =
            normalized.scheduledAtMillis?.takeIf { it > now }
                ?: if (normalized.delaySeconds > 0) {
                    now + normalized.delaySeconds * 1000L
                } else {
                    null
                }
        if (atMillis == null) {
            FakeCallNotifier.show(context, normalized.callerName)
            return
        }
        addAndArm(context, normalized.callerName, atMillis)
    }

    fun cancel(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        migrateLegacyIfNeeded(context, prefs)
        val list = readSchedulesJson(prefs)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (call in list) {
            am.cancel(pendingIntent(context, call))
            pendingIntent(context, call).cancel()
        }
        prefs.edit().putString(KEY_SCHEDULES_JSON, "[]").apply()
    }

    fun cancelSchedule(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        migrateLegacyIfNeeded(context, prefs)
        val list = readSchedulesJson(prefs).toMutableList()
        val found = list.find { it.id == id } ?: return
        list.remove(found)
        writeSchedulesJson(prefs, list)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(context, found))
        pendingIntent(context, found).cancel()
    }

    fun getUpcomingSchedules(context: Context): List<ScheduledCall> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        migrateLegacyIfNeeded(context, prefs)
        pruneExpired(context, prefs)
        return readSchedulesJson(prefs)
            .filter { it.triggerAtMillis > System.currentTimeMillis() }
            .sortedBy { it.triggerAtMillis }
    }

    fun describeScheduledCall(context: Context): String? {
        val upcoming = getUpcomingSchedules(context)
        if (upcoming.isEmpty()) return null
        if (upcoming.size == 1) {
            val c = upcoming.first()
            return context.getString(
                R.string.phone_scheduled_summary,
                c.callerName,
                formatWhen(context, c.triggerAtMillis),
            )
        }
        val first = upcoming.first()
        return context.getString(
            R.string.phone_scheduled_multi_summary,
            upcoming.size,
            first.callerName,
            formatWhen(context, first.triggerAtMillis),
        )
    }

    fun onAlarmFired(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = readSchedulesJson(prefs).toMutableList()
        val scheduleId = intent.getStringExtra(FakeIncomingCallActivity.EXTRA_SCHEDULE_ID)
        val intentCaller =
            intent.getStringExtra(FakeIncomingCallActivity.EXTRA_CALLER)?.trim().orEmpty()
        val found: ScheduledCall? =
            when {
                scheduleId != null -> list.find { it.id == scheduleId }
                list.size == 1 -> list.first()
                else -> null
            }
        if (found != null) {
            list.remove(found)
            writeSchedulesJson(prefs, list)
        }
        val caller =
            found?.callerName?.trim()?.ifBlank { null }
                ?: intentCaller.ifBlank { null }
                ?: context.getString(R.string.fake_call_default_name)
        FakeCallNotifier.show(context, caller)
    }

    private fun addAndArm(context: Context, callerName: String, triggerAt: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        migrateLegacyIfNeeded(context, prefs)
        val id = java.util.UUID.randomUUID().toString()
        val rc = nextRequestCode(prefs)
        val call = ScheduledCall(id = id, callerName = callerName, triggerAtMillis = triggerAt, requestCode = rc)
        val list = readSchedulesJson(prefs).toMutableList()
        list.add(call)
        writeSchedulesJson(prefs, list.sortedBy { it.triggerAtMillis })
        armAlarm(context, call)
    }

    private fun armAlarm(context: Context, call: ScheduledCall) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, call)
        val triggerAt = call.triggerAtMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(context: Context, call: ScheduledCall): PendingIntent {
        val intent =
            Intent(context, FakeCallAlarmReceiver::class.java).apply {
                putExtra(FakeIncomingCallActivity.EXTRA_CALLER, call.callerName)
                putExtra(FakeIncomingCallActivity.EXTRA_SCHEDULE_ID, call.id)
            }
        return PendingIntent.getBroadcast(
            context,
            call.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun sanitizeCaller(context: Context, callerName: String): String {
        return callerName.trim().ifBlank { context.getString(R.string.fake_call_default_name) }
    }

    private fun migrateLegacyIfNeeded(context: Context, prefs: SharedPreferences) {
        if (prefs.contains(KEY_SCHEDULES_JSON)) return
        cancelLegacySingleAlarm(context)
        val legacyCaller = prefs.getString(LEGACY_KEY_CALLER, null)
        val legacyAt = prefs.getLong(LEGACY_KEY_TRIGGER_AT, 0L)
        cancelLegacySingleAlarm(context)
        prefs.edit().remove(LEGACY_KEY_CALLER).remove(LEGACY_KEY_TRIGGER_AT).apply()
        if (legacyCaller != null && legacyAt > System.currentTimeMillis()) {
            val id = java.util.UUID.randomUUID().toString()
            val rc = nextRequestCode(prefs)
            val call =
                ScheduledCall(
                    id = id,
                    callerName = legacyCaller,
                    triggerAtMillis = legacyAt,
                    requestCode = rc,
                )
            writeSchedulesJson(prefs, mutableListOf(call))
            armAlarm(context, call)
        } else {
            writeSchedulesJson(prefs, mutableListOf())
        }
    }

    private fun cancelLegacySingleAlarm(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val legacyPi =
            PendingIntent.getBroadcast(
                context,
                LEGACY_REQUEST_CODE,
                Intent(context, FakeCallAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        legacyPi?.let {
            am.cancel(it)
            it.cancel()
        }
    }

    private fun pruneExpired(context: Context, prefs: SharedPreferences) {
        val now = System.currentTimeMillis()
        val list = readSchedulesJson(prefs)
        val (future, past) = list.partition { it.triggerAtMillis > now }
        if (past.isEmpty()) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (call in past) {
            am.cancel(pendingIntent(context, call))
            pendingIntent(context, call).cancel()
        }
        writeSchedulesJson(prefs, future.toMutableList())
    }

    private fun nextRequestCode(prefs: SharedPreferences): Int {
        synchronized(this) {
            var next = prefs.getInt(KEY_NEXT_REQUEST_CODE, BASE_REQUEST_CODE)
            if (next < BASE_REQUEST_CODE) next = BASE_REQUEST_CODE
            prefs.edit().putInt(KEY_NEXT_REQUEST_CODE, next + 1).apply()
            return next
        }
    }

    private fun readSchedulesJson(prefs: SharedPreferences): List<ScheduledCall> {
        val raw = prefs.getString(KEY_SCHEDULES_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ScheduledCall(
                            id = o.getString("id"),
                            callerName = o.getString("caller"),
                            triggerAtMillis = o.getLong("at"),
                            requestCode = o.getInt("rc"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeSchedulesJson(prefs: SharedPreferences, list: List<ScheduledCall>) {
        val arr = JSONArray()
        for (c in list) {
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("caller", c.callerName)
                    .put("at", c.triggerAtMillis)
                    .put("rc", c.requestCode),
            )
        }
        prefs.edit().putString(KEY_SCHEDULES_JSON, arr.toString()).apply()
    }

    private fun formatWhen(context: Context, triggerAt: Long): String {
        return android.text.format.DateUtils.formatDateTime(
            context,
            triggerAt,
            android.text.format.DateUtils.FORMAT_SHOW_TIME or
                android.text.format.DateUtils.FORMAT_SHOW_DATE or
                android.text.format.DateUtils.FORMAT_ABBREV_ALL,
        )
    }
}
