package dev.arpan.calling

import android.graphics.drawable.AnimationDrawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import dev.arpan.calling.databinding.ActivityActiveCallBinding
import dev.arpan.calling.databinding.ActivityFakeIncomingCallBinding

/**
 * Applies [FakeCallScreenThemeStore] and [FakeCallScreenThemeStore.CallLayoutStyle] to the
 * full-screen fake call UI (incoming + active).
 */
object CallScreenStyleApplier {
    fun pauseRootBackgroundAnimation(root: View) {
        (root.background as? AnimationDrawable)?.stop()
    }

    fun startRootBackgroundAnimationIfRunning(root: View) {
        (root.background as? AnimationDrawable)?.start()
    }

    fun apply(
        binding: ActivityFakeIncomingCallBinding,
        activeBinding: ActivityActiveCallBinding?,
        systemInsetTop: Int = 0,
        systemInsetBottom: Int = 0,
    ) {
        val context = binding.root.context
        val style = resolveEffectiveBackgroundStyle(context)
        val layoutStyle = FakeCallScreenThemeStore.getLayoutStyle(context)
        val incomingBrand = FakeCallScreenThemeStore.getIncomingUiBrand(context)

        pauseRootBackgroundAnimation(binding.callScreensRoot)

        binding.callBackgroundImage.setImageDrawable(null)
        binding.callBackgroundImage.visibility = View.GONE
        binding.callBackgroundScrim.visibility = View.GONE

        when (style) {
            FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT -> {
                binding.callScreensRoot.setBackgroundResource(defaultMovingGradientDrawable(incomingBrand))
            }
            FakeCallScreenThemeStore.CallBackgroundStyle.DARK -> {
                binding.callScreensRoot.setBackgroundResource(R.drawable.bg_call_screen_dark_static)
            }
            FakeCallScreenThemeStore.CallBackgroundStyle.CUSTOM_GALLERY -> {
                binding.callScreensRoot.background = null
                val bmp = CallBackgroundImageStore.decodeIfPresent(context)
                if (bmp != null) {
                    binding.callBackgroundImage.setImageBitmap(bmp)
                    binding.callBackgroundImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    binding.callBackgroundImage.visibility = View.VISIBLE
                    binding.callBackgroundScrim.visibility = View.VISIBLE
                } else {
                    binding.callScreensRoot.setBackgroundResource(defaultMovingGradientDrawable(incomingBrand))
                }
            }
            FakeCallScreenThemeStore.CallBackgroundStyle.CONTACT_PHOTO_FOCUS -> {
                binding.callScreensRoot.background = null
                val bmp = CallerAvatarStore.decodeStoredAvatarBitmap(context)
                if (bmp != null) {
                    binding.callBackgroundImage.setImageBitmap(bmp)
                    binding.callBackgroundImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    binding.callBackgroundImage.visibility = View.VISIBLE
                    binding.callBackgroundScrim.visibility = View.VISIBLE
                } else {
                    binding.callScreensRoot.setBackgroundResource(defaultMovingGradientDrawable(incomingBrand))
                }
            }
        }

        applyIncomingLayout(binding, layoutStyle, systemInsetTop, systemInsetBottom)
        activeBinding?.let { applyActiveLayout(it, layoutStyle, systemInsetTop) }
    }

    private fun defaultMovingGradientDrawable(brand: FakeCallScreenThemeStore.IncomingCallUiBrand): Int =
        when (brand) {
            FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_ONE_UI,
            FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_SWIPE_UP,
            -> R.drawable.anim_samsung_call_screen_gradient
            FakeCallScreenThemeStore.IncomingCallUiBrand.ONEPLUS ->
                R.drawable.bg_oneplus_solid
            FakeCallScreenThemeStore.IncomingCallUiBrand.IPHONE ->
                R.drawable.bg_iphone_solid
        }

    private fun resolveEffectiveBackgroundStyle(context: android.content.Context): FakeCallScreenThemeStore.CallBackgroundStyle {
        val requested = FakeCallScreenThemeStore.getBackgroundStyle(context)
        return when (requested) {
            FakeCallScreenThemeStore.CallBackgroundStyle.CUSTOM_GALLERY ->
                if (CallBackgroundImageStore.hasCustomBackground(context)) {
                    requested
                } else {
                    FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT
                }
            FakeCallScreenThemeStore.CallBackgroundStyle.CONTACT_PHOTO_FOCUS ->
                if (CallerAvatarStore.hasCustomAvatar(context)) {
                    requested
                } else {
                    FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT
                }
            else -> requested
        }
    }

    private fun applyIncomingLayout(
        binding: ActivityFakeIncomingCallBinding,
        layoutStyle: FakeCallScreenThemeStore.CallLayoutStyle,
        systemInsetTop: Int,
        systemInsetBottom: Int,
    ) {
        val context = binding.root.context
        when (FakeCallScreenThemeStore.getIncomingUiBrand(context)) {
            FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_ONE_UI,
            FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_SWIPE_UP,
            -> applySamsungIncomingLayout(binding, layoutStyle, systemInsetTop, systemInsetBottom)
            FakeCallScreenThemeStore.IncomingCallUiBrand.ONEPLUS ->
                applyOnePlusIncomingLayout(binding, layoutStyle, systemInsetTop, systemInsetBottom)
            FakeCallScreenThemeStore.IncomingCallUiBrand.IPHONE ->
                applyIphoneIncomingLayout(binding, layoutStyle, systemInsetTop, systemInsetBottom)
        }
    }

    private fun applySamsungIncomingLayout(
        binding: ActivityFakeIncomingCallBinding,
        layoutStyle: FakeCallScreenThemeStore.CallLayoutStyle,
        systemInsetTop: Int,
        systemInsetBottom: Int,
    ) {
        val res = binding.root.resources
        val topRowPx =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD ->
                    res.getDimensionPixelSize(R.dimen.samsung_incoming_top_margin)
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                    res.getDimensionPixelSize(R.dimen.samsung_incoming_top_margin_compact)
            } + systemInsetTop
        (binding.samsungHdRow.layoutParams as ConstraintLayout.LayoutParams).topMargin = topRowPx
        binding.samsungHdRow.requestLayout()

        val nameSizePx =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD ->
                    res.getDimension(R.dimen.samsung_caller_name_sp)
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                    res.getDimension(R.dimen.samsung_caller_name_sp_compact)
            }
        binding.samsungCallerName.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameSizePx)
        applyBottomInsetSpacer(binding.samsungHomeBar, systemInsetBottom)
    }

    private fun applyOnePlusIncomingLayout(
        binding: ActivityFakeIncomingCallBinding,
        layoutStyle: FakeCallScreenThemeStore.CallLayoutStyle,
        systemInsetTop: Int,
        systemInsetBottom: Int,
    ) {
        val res = binding.root.resources
        val topFromPx =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD ->
                    res.getDimensionPixelSize(R.dimen.oneplus_name_top_margin)
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                    res.getDimensionPixelSize(R.dimen.oneplus_name_top_margin_compact)
            } + systemInsetTop
        (binding.onePlusCallFrom.layoutParams as ConstraintLayout.LayoutParams).topMargin = topFromPx
        binding.onePlusCallFrom.requestLayout()

        val nameSp =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD -> 36f
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT -> 30f
            }
        binding.onePlusCallerName.setTextSize(TypedValue.COMPLEX_UNIT_SP, nameSp)
        applyBottomInsetSpacer(binding.onePlusHomeBar, systemInsetBottom)
    }

    private fun applyIphoneIncomingLayout(
        binding: ActivityFakeIncomingCallBinding,
        layoutStyle: FakeCallScreenThemeStore.CallLayoutStyle,
        systemInsetTop: Int,
        systemInsetBottom: Int,
    ) {
        val res = binding.root.resources
        val topFromPx =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD ->
                    res.getDimensionPixelSize(R.dimen.iphone_name_top_margin)
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                    res.getDimensionPixelSize(R.dimen.iphone_name_top_margin_compact)
            } + systemInsetTop
        (binding.iphoneIncomingLabel.layoutParams as ConstraintLayout.LayoutParams).topMargin = topFromPx
        binding.iphoneIncomingLabel.requestLayout()

        val nameSp =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD -> 34f
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT -> 28f
            }
        binding.iphoneCallerName.setTextSize(TypedValue.COMPLEX_UNIT_SP, nameSp)
        applyBottomInsetSpacer(binding.iphoneHomeBar, systemInsetBottom)
    }

    private fun applyBottomInsetSpacer(spacer: View, systemInsetBottom: Int) {
        val min = spacer.resources.getDimensionPixelSize(R.dimen.call_home_bar_bottom_margin)
        val lp = spacer.layoutParams as ConstraintLayout.LayoutParams
        lp.bottomMargin = maxOf(systemInsetBottom, min)
        spacer.layoutParams = lp
    }

    private fun applyActiveLayout(
        active: ActivityActiveCallBinding,
        layoutStyle: FakeCallScreenThemeStore.CallLayoutStyle,
        systemInsetTop: Int,
    ) {
        val res = active.root.resources
        val topMarginPx =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD -> res.getDimensionPixelSize(R.dimen.call_top_margin)
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                    res.getDimensionPixelSize(R.dimen.call_top_margin_compact)
            } + systemInsetTop
        val avatarTopPx =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD ->
                    res.getDimensionPixelSize(R.dimen.call_avatar_top_margin)
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                    res.getDimensionPixelSize(R.dimen.call_avatar_top_margin_compact)
            }
        val avatarSizePx =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD ->
                    res.getDimensionPixelSize(R.dimen.call_avatar_size)
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                    res.getDimensionPixelSize(R.dimen.call_avatar_size_compact)
            }
        val nameSp =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD -> 32f
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT -> 27f
            }

        (active.onCallLabel.layoutParams as ConstraintLayout.LayoutParams).topMargin = topMarginPx
        active.onCallLabel.requestLayout()

        active.callerName.setTextSize(TypedValue.COMPLEX_UNIT_SP, nameSp)

        val avatarLp = active.avatarCard.layoutParams as ConstraintLayout.LayoutParams
        avatarLp.height = avatarSizePx
        avatarLp.width = avatarSizePx
        avatarLp.topMargin = avatarTopPx
        active.avatarCard.radius = avatarSizePx / 2f
        active.avatarCard.layoutParams = avatarLp

        val controlsTopPx =
            when (layoutStyle) {
                FakeCallScreenThemeStore.CallLayoutStyle.STANDARD ->
                    res.getDimensionPixelSize(R.dimen.call_controls_top_margin)
                FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                    res.getDimensionPixelSize(R.dimen.call_controls_top_margin_compact)
            }
        (active.muteButton.layoutParams as ConstraintLayout.LayoutParams).topMargin = controlsTopPx
        active.muteButton.requestLayout()
    }
}
