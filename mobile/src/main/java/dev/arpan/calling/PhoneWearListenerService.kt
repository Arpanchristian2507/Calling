package dev.arpan.calling

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class PhoneWearListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearLink.MESSAGE_PATH_TRIGGER) return
        val request = FakeCallRequest.decode(event.data)
        FakeCallScheduler.dispatch(this, request)
    }
}
