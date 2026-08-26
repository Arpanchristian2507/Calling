package dev.arpan.calling

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import dev.arpan.calling.databinding.ItemCallThemeCardBinding

class CallThemesAdapter(
    private var items: List<ThemeCardItem>,
    private var selectedBrand: FakeCallScreenThemeStore.IncomingCallUiBrand,
    private val onIconAction: (ThemeIconAction) -> Unit,
) : RecyclerView.Adapter<CallThemesAdapter.VH>() {

    enum class ThemeIconAction {
        PREVIEW,
        EDIT,
    }

    data class ThemeCardItem(
        @StringRes val titleRes: Int,
        val brand: FakeCallScreenThemeStore.IncomingCallUiBrand,
        @DrawableRes val previewRes: Int,
    )

    fun submitList(newItems: List<ThemeCardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun selectedBrand(): FakeCallScreenThemeStore.IncomingCallUiBrand = selectedBrand

    fun setSelectedBrand(brand: FakeCallScreenThemeStore.IncomingCallUiBrand) {
        selectedBrand = brand
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCallThemeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(
        private val binding: ItemCallThemeCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ThemeCardItem) {
            binding.themeTitle.setText(item.titleRes)
            binding.themePreview.setImageResource(item.previewRes)
            val selected = item.brand == selectedBrand
            binding.themeSelected.isChecked = selected
            val strokePx =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    if (selected) 2f else 0f,
                    binding.root.resources.displayMetrics,
                ).toInt()
            (binding.root as MaterialCardView).strokeWidth = strokePx

            binding.root.setOnClickListener {
                selectedBrand = item.brand
                notifyDataSetChanged()
            }
            binding.themePreviewEye.setOnClickListener {
                onIconAction(ThemeIconAction.PREVIEW)
            }
            binding.themeEdit.setOnClickListener {
                onIconAction(ThemeIconAction.EDIT)
            }
        }
    }
}
