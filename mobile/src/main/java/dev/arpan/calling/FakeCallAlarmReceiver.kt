package dev.arpan.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FakeCallAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FakeCallScheduler.onAlarmFired(context.applicationContext, intent)
    }
}
