package dev.arpan.calling

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * [FrameLayout] used with touch listeners for gesture handling; overrides [performClick]
 * so accessibility services and lint's clickable-view checks align with manual touch handling.
 */
internal class AccessibleTouchFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    override fun performClick(): Boolean = super.performClick()

    fun bindTouchListener(listener: OnTouchListener) {
        setOnTouchListener(listener)
    }
}
