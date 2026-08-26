package dev.arpan.calling

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.arpan.calling.databinding.ItemScheduledCallBinding

internal class ScheduleCallAdapter(
    private val onCancel: (ScheduledCall) -> Unit,
) : RecyclerView.Adapter<ScheduleCallAdapter.VH>() {
    private val items = mutableListOf<ScheduledCall>()

    fun submit(list: List<ScheduledCall>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ItemScheduledCallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onCancel)
    }

    override fun getItemCount(): Int = items.size

    class VH(
        private val binding: ItemScheduledCallBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            call: ScheduledCall,
            onCancel: (ScheduledCall) -> Unit,
        ) {
            val context = binding.root.context
            binding.callerName.text = call.callerName
            binding.scheduledWhen.text =
                android.text.format.DateUtils.formatDateTime(
                    context,
                    call.triggerAtMillis,
                    android.text.format.DateUtils.FORMAT_SHOW_TIME or
                        android.text.format.DateUtils.FORMAT_SHOW_DATE or
                        android.text.format.DateUtils.FORMAT_ABBREV_ALL,
                )
            binding.cancelButton.setOnClickListener { onCancel(call) }
        }
    }
}
