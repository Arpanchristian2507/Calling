package com.example.calling

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calling.databinding.ActivityFakeIncomingCallBinding
import java.util.Locale

class FakeIncomingCallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFakeIncomingCallBinding
    private val timerHandler = Handler(Looper.getMainLooper())
    private var callStartElapsed: Long = 0L
    private var isCallAnswered: Boolean = false
    private val timerRunnable =
        object : Runnable {
            override fun run() {
                updateCallTimer()
                timerHandler.postDelayed(this, ONE_SECOND_MS)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FakeCallNotifier.cancel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        binding = ActivityFakeIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val caller = intent.getStringExtra(EXTRA_CALLER) ?: getString(R.string.fake_call_default_name)
        binding.callerName.text = caller
        showIncomingState()

        binding.decline.setOnClickListener {
            FakeCallRinger.stop()
            finish()
        }
        binding.answer.setOnClickListener {
            FakeCallRinger.stop()
            showAnsweredState()
        }
        binding.endCall.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (isCallAnswered) {
            startTimer()
        } else {
            FakeCallRinger.start(this)
        }
    }

    override fun onStop() {
        FakeCallRinger.stop()
        stopTimer()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val caller = intent.getStringExtra(EXTRA_CALLER) ?: getString(R.string.fake_call_default_name)
        if (::binding.isInitialized) {
            binding.callerName.text = caller
            showIncomingState()
        }
        FakeCallRinger.start(this)
    }

    private fun showIncomingState() {
        isCallAnswered = false
        stopTimer()
        binding.incomingLabel.text = getString(R.string.fake_call_incoming_label)
        binding.subtitle.text = getString(R.string.fake_call_subtitle)

        binding.decline.visibility = View.VISIBLE
        binding.answer.visibility = View.VISIBLE
        binding.endCall.visibility = View.GONE
        setInCallControlsVisibility(View.GONE)
    }

    private fun showAnsweredState() {
        isCallAnswered = true
        callStartElapsed = SystemClock.elapsedRealtime()
        binding.incomingLabel.text = getString(R.string.fake_call_connected_label)
        binding.subtitle.text = getString(R.string.fake_call_connected_timer)

        binding.decline.visibility = View.GONE
        binding.answer.visibility = View.GONE
        binding.endCall.visibility = View.VISIBLE
        setInCallControlsVisibility(View.VISIBLE)
        startTimer()
    }

    private fun setInCallControlsVisibility(visibility: Int) {
        binding.mute.visibility = visibility
        binding.keypad.visibility = visibility
        binding.speaker.visibility = visibility
        binding.muteLabel.visibility = visibility
        binding.keypadLabel.visibility = visibility
        binding.speakerLabel.visibility = visibility
    }

    private fun startTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        updateCallTimer()
        timerHandler.postDelayed(timerRunnable, ONE_SECOND_MS)
    }

    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateCallTimer() {
        if (!isCallAnswered || callStartElapsed == 0L) return

        val elapsedSeconds = ((SystemClock.elapsedRealtime() - callStartElapsed) / ONE_SECOND_MS).toInt()
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        binding.subtitle.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    companion object {
        const val EXTRA_CALLER: String = "extra_caller"
        private const val ONE_SECOND_MS: Long = 1000L
    }
}
