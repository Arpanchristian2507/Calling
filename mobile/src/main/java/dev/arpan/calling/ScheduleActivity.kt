package dev.arpan.calling

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.arpan.calling.databinding.ActivityScheduleBinding

class ScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScheduleBinding
    private val adapter =
        ScheduleCallAdapter { call ->
            FakeCallScheduler.cancelSchedule(this, call.id)
            refreshList()
            Toast.makeText(this, getString(R.string.schedule_cancelled_one), Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.scheduleList.adapter = adapter
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val upcoming = FakeCallScheduler.getUpcomingSchedules(this)
        adapter.submit(upcoming)
        binding.scheduleEmpty.visibility = if (upcoming.isEmpty()) View.VISIBLE else View.GONE
        binding.scheduleList.visibility = if (upcoming.isEmpty()) View.GONE else View.VISIBLE
    }
}
