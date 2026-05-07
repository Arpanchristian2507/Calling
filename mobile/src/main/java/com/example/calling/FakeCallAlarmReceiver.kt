package com.example.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FakeCallAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FakeCallScheduler.clearScheduledState(context)
        val caller =
            intent.getStringExtra(FakeIncomingCallActivity.EXTRA_CALLER)
                ?.trim()
                .orEmpty()
                .ifBlank { context.getString(R.string.fake_call_default_name) }
        FakeCallNotifier.show(context, caller)
    }
}
