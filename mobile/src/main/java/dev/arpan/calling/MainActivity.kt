package dev.arpan.calling

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dev.arpan.calling.databinding.ActivityMainBinding
import java.util.Calendar
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val pickCallerPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@registerForActivityResult
            if (CallerAvatarStore.saveFromGalleryUri(this, uri)) {
                toast(getString(R.string.phone_caller_photo_saved))
            } else {
                toast(getString(R.string.phone_caller_photo_failed))
            }
            refreshCallerPhotoUi()
        }

    private val pickCallerVoice =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            if (CallerVoiceStore.saveFromUri(this, uri)) {
                toast(getString(R.string.phone_voice_saved))
            } else {
                toast(getString(R.string.phone_voice_failed))
            }
            refreshCallerVoiceUi()
        }

    /** Wall-clock instant for the "pick time" delay option; null when using relative delays. */
    private var clockRingAtMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupDelayButtons()
        binding.callerPhotoCard.setOnClickListener {
            pickCallerPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
        binding.callerPhotoMinus.setOnClickListener {
            CallerAvatarStore.clear(this)
            toast(getString(R.string.phone_caller_photo_cleared))
            refreshCallerPhotoUi()
        }
        binding.callerVoiceCard.setOnClickListener {
            pickCallerVoice.launch("audio/*")
        }
        binding.callerVoiceMinus.setOnClickListener {
            CallerVoiceStore.clear(this)
            toast(getString(R.string.phone_voice_cleared))
            refreshCallerVoiceUi()
        }
        binding.bottomNavCall.setOnClickListener {
            dispatchSelectedRequest()
        }
        binding.bottomNavSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
        binding.bottomNavSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.cancelScheduled.setOnClickListener {
            FakeCallScheduler.cancel(this)
            refreshScheduledState()
            toast(getString(R.string.phone_cancelled))
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshScheduledState()
        refreshCallerPhotoUi()
        refreshCallerVoiceUi()
    }

    private fun relativeDelayButtons() =
        listOf(
            binding.delayInstant,
            binding.delay5s,
            binding.delay15s,
            binding.delay30s,
            binding.delay1m,
            binding.delay5m,
            binding.delay10m,
        )

    private fun setupDelayButtons() {
        val relative = relativeDelayButtons()
        relative.forEach { button ->
            button.setOnClickListener {
                clockRingAtMillis = null
                binding.delayPickTime.text = ""
                relative.forEach { candidate ->
                    candidate.isChecked = candidate === button
                }
                binding.delayPickTime.isChecked = false
            }
        }
        binding.delay5s.isChecked = true

        binding.delayPickTime.setOnClickListener {
            showRingTimePicker()
        }
    }

    private fun showRingTimePicker() {
        val cal = Calendar.getInstance()
        clockRingAtMillis?.let { at ->
            cal.timeInMillis = at
        }
        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(cal.get(Calendar.HOUR_OF_DAY))
                .setMinute(cal.get(Calendar.MINUTE))
                .setTitleText(getString(R.string.phone_delay_pick_time_title))
                .build()
        picker.addOnPositiveButtonClickListener {
            val atMillis = computeNextRingMillis(picker.hour, picker.minute)
            clockRingAtMillis = atMillis
            relativeDelayButtons().forEach { it.isChecked = false }
            binding.delayPickTime.isChecked = true
            binding.delayPickTime.text =
                android.text.format.DateFormat.getTimeFormat(this@MainActivity)
                    .format(Date(atMillis))
        }
        picker.show(supportFragmentManager, "ring_time")
    }

    private fun computeNextRingMillis(hourOfDay: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun dispatchSelectedRequest() {
        val caller = binding.callerNameInput.text?.toString().orEmpty()
        val request = buildFakeCallRequest(caller) ?: run {
            toast(getString(R.string.phone_pick_future_time))
            return
        }
        FakeCallScheduler.dispatch(this, request)
        refreshScheduledState()
        val displayName = request.callerName.ifBlank { getString(R.string.fake_call_default_name) }
        val message =
            when {
                request.scheduledAtMillis != null &&
                    request.scheduledAtMillis > System.currentTimeMillis() -> {
                    val timeStr =
                        android.text.format.DateFormat.getTimeFormat(this)
                            .format(Date(request.scheduledAtMillis))
                    getString(R.string.phone_triggered_at_time, displayName, timeStr)
                }
                request.delaySeconds <= 0 ->
                    getString(R.string.phone_triggered_now, displayName)
                else ->
                    getString(R.string.phone_triggered_delay, displayName, request.delaySeconds)
            }
        toast(message)
    }

    private fun buildFakeCallRequest(caller: String): FakeCallRequest? {
        val clock = clockRingAtMillis
        if (binding.delayPickTime.isChecked) {
            if (clock == null || clock <= System.currentTimeMillis()) {
                return null
            }
            return FakeCallRequest(callerName = caller, delaySeconds = 0, scheduledAtMillis = clock)
        }
        return FakeCallRequest(callerName = caller, delaySeconds = selectedRelativeDelaySeconds(), scheduledAtMillis = null)
    }

    private fun selectedRelativeDelaySeconds(): Int {
        return when {
            binding.delayInstant.isChecked -> 0
            binding.delay5s.isChecked -> 5
            binding.delay15s.isChecked -> 15
            binding.delay30s.isChecked -> 30
            binding.delay1m.isChecked -> 60
            binding.delay5m.isChecked -> 300
            binding.delay10m.isChecked -> 600
            else -> 5
        }
    }

    private fun refreshScheduledState() {
        binding.scheduledStatus.text =
            FakeCallScheduler.describeScheduledCall(this)
                ?: getString(R.string.phone_scheduled_none)
    }

    private fun refreshCallerPhotoUi() {
        CallerAvatarStore.apply(
            this,
            binding.callerPhotoPreview,
            CallerAvatarStore.AvatarStyle.MAIN_PREVIEW,
        )
        val has = CallerAvatarStore.hasCustomAvatar(this)
        binding.callerPhotoMinus.visibility = if (has) View.VISIBLE else View.GONE
        binding.callerPhotoPlus.visibility = if (has) View.GONE else View.VISIBLE
    }

    private fun refreshCallerVoiceUi() {
        val has = CallerVoiceStore.hasCustomVoice(this)
        binding.callerVoiceMinus.visibility = if (has) View.VISIBLE else View.GONE
        binding.callerVoicePlus.visibility = if (has) View.GONE else View.VISIBLE
        val tint =
            if (has) {
                ContextCompat.getColorStateList(this, R.color.calling_primary)
            } else {
                ContextCompat.getColorStateList(this, R.color.calling_on_surface_variant)
            }
        binding.callerVoiceIcon.imageTintList = tint
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
