package com.example.calling

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calling.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
        setupDelayChips()
        binding.triggerNow.setOnClickListener {
            dispatchSelectedRequest()
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
    }

    private fun setupDelayChips() {
        val buttons =
            listOf(
                binding.delayInstant,
                binding.delay5s,
                binding.delay15s,
                binding.delay30s,
            )
        buttons.forEach { button ->
            button.setOnClickListener {
                buttons.forEach { candidate ->
                    candidate.isChecked = candidate === button
                }
            }
        }
        binding.delay5s.isChecked = true
    }

    private fun dispatchSelectedRequest() {
        val caller = binding.callerNameInput.text?.toString().orEmpty()
        val request = FakeCallRequest(callerName = caller, delaySeconds = selectedDelay())
        FakeCallScheduler.dispatch(this, request)
        refreshScheduledState()
        val message =
            if (request.delaySeconds <= 0) {
                getString(
                    R.string.phone_triggered_now,
                    request.callerName.ifBlank { getString(R.string.fake_call_default_name) },
                )
            } else {
                getString(
                    R.string.phone_triggered_delay,
                    request.callerName.ifBlank { getString(R.string.fake_call_default_name) },
                    request.delaySeconds,
                )
            }
        toast(message)
    }

    private fun selectedDelay(): Int {
        return when {
            binding.delayInstant.isChecked -> 0
            binding.delay15s.isChecked -> 15
            binding.delay30s.isChecked -> 30
            else -> 5
        }
    }

    private fun refreshScheduledState() {
        binding.scheduledStatus.text =
            FakeCallScheduler.describeScheduledCall(this)
                ?: getString(R.string.phone_scheduled_none)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
