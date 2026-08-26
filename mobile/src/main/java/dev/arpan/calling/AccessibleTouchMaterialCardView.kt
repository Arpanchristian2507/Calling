package dev.arpan.calling

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView

/**
 * [MaterialCardView] used with [setOnTouchListener] for gesture handling; overrides
 * [performClick] so accessibility services and Android lint's clickable-view checks align
 * with manual touch handling.
 */
internal class AccessibleTouchMaterialCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle,
) : MaterialCardView(context, attrs, defStyleAttr) {
    override fun performClick(): Boolean = super.performClick()

    fun bindTouchListener(listener: OnTouchListener) {
        setOnTouchListener(listener)
    }
}
