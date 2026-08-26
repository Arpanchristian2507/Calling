package dev.arpan.calling

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import dev.arpan.calling.databinding.ActivityFakeIncomingCallBinding

/**
 * Optional One UI–style **visual** layer for the Samsung incoming pane when the user picks
 * [FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_ONE_UI] in Call Themes.
 *
 * **Pickup gesture** (omnidirectional drag with progress-driven ripples / glow) lives in
 * [FakeIncomingCallActivity.attachSamsungOmnidirectionalSwipePickup]. This class only handles optional button
 * press scaling, success flashes, and blur on aura views; ripples stay hidden until the user
 * drags answer or decline.
 *
 * For [FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_SWIPE_UP], overlays stay hidden and
 * [GestureVisualSink.NONE] is used.
 */
class SamsungIncomingCallTheme(
    private val activity: FakeIncomingCallActivity,
    private val binding: ActivityFakeIncomingCallBinding,
) {
    /** Forwards optional visuals from the swipe gesture without changing gesture logic. */
    interface GestureVisualSink {
        fun onTouchDown(button: View)

        fun onTouchUp(button: View)

        /** Invoked after the hold completed successfully; run [onComplete] after any flash. */
        fun onSwipeSucceeded(isAnswer: Boolean, button: View, onComplete: () -> Unit)

        companion object {
            val NONE: GestureVisualSink =
                object : GestureVisualSink {
                    override fun onTouchDown(button: View) {}

                    override fun onTouchUp(button: View) {}

                    override fun onSwipeSucceeded(isAnswer: Boolean, button: View, onComplete: () -> Unit) {
                        onComplete()
                    }
                }
        }
    }

    val gestureVisualSink: GestureVisualSink =
        object : GestureVisualSink {
            override fun onTouchDown(button: View) {
                onButtonTouchDown(button)
            }

            override fun onTouchUp(button: View) {
                onButtonTouchUp(button)
            }

            override fun onSwipeSucceeded(isAnswer: Boolean, button: View, onComplete: () -> Unit) {
                if (isAnswer) {
                    playAnswerSuccess(button, onComplete)
                } else {
                    playRejectSuccess(button, onComplete)
                }
            }
        }

    fun start() {
        hideVisualOverlays(binding)
        binding.incomingSamsungPane.post {
            prepareRipplePivots(binding.samsungAnswerRipple1)
            prepareRipplePivots(binding.samsungAnswerRipple2)
            prepareRipplePivots(binding.samsungDeclineRipple1)
            prepareRipplePivots(binding.samsungDeclineRipple2)
        }
        applyAuraBlurIfSupported()
    }

    fun stop() {
    }

    private fun applyAuraBlurIfSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val blur = RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
        binding.samsungAnswerAura.setRenderEffect(blur)
        binding.samsungDeclineAura.setRenderEffect(blur)
    }

    private fun prepareRipplePivots(v: View) {
        if (v.width == 0) return
        v.pivotX = v.width / 2f
        v.pivotY = v.height / 2f
    }

    private fun onButtonTouchDown(button: View) {
        button.animate().cancel()
        button
            .animate()
            .scaleX(TOUCH_PRESS_SCALE)
            .scaleY(TOUCH_PRESS_SCALE)
            .setDuration(TOUCH_DOWN_MS)
            .start()
    }

    private fun onButtonTouchUp(button: View) {
        button
            .animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(TOUCH_UP_MS)
            .start()
    }

    private fun playAnswerSuccess(button: View, then: () -> Unit) {
        button.animate().cancel()
        val flash = binding.samsungAnswerSuccessFlash
        flash.visibility = View.VISIBLE
        flash.alpha = 0f
        flash.scaleX = 0.35f
        flash.scaleY = 0.35f
        flash.pivotX = button.x + button.width / 2f
        flash.pivotY = button.y + button.height / 2f

        button.pivotX = button.width / 2f
        button.pivotY = button.height / 2f
        button.scaleX = TOUCH_PRESS_SCALE
        button.scaleY = TOUCH_PRESS_SCALE
        button
            .animate()
            .scaleX(ANSWER_BURST_TARGET_SCALE)
            .scaleY(ANSWER_BURST_TARGET_SCALE)
            .setDuration(ANSWER_BURST_MS)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                flash.alpha = 0.95f
                flash
                    .animate()
                    .scaleX(ANSWER_FLASH_EXPAND_SCALE)
                    .scaleY(ANSWER_FLASH_EXPAND_SCALE)
                    .alpha(1f)
                    .setDuration(ANSWER_FLASH_EXPAND_MS)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction {
                        flash
                            .animate()
                            .alpha(0f)
                            .setDuration(ANSWER_FLASH_FADE_MS)
                            .withEndAction {
                                flash.visibility = View.GONE
                                flash.scaleX = 1f
                                flash.scaleY = 1f
                                flash.alpha = 0f
                                button.scaleX = 1f
                                button.scaleY = 1f
                                then()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun playRejectSuccess(button: View, then: () -> Unit) {
        button.animate().cancel()
        val flash = binding.samsungRejectSuccessFlash
        flash.visibility = View.VISIBLE
        flash.alpha = 0.4f
        flash.scaleX = 0.4f
        flash.scaleY = 0.4f
        flash.pivotX = button.x + button.width / 2f
        flash.pivotY = button.y + button.height / 2f

        button.pivotX = button.width / 2f
        button.pivotY = button.height / 2f
        flash
            .animate()
            .scaleX(16f)
            .scaleY(16f)
            .alpha(0.85f)
            .setDuration(220L)
            .withEndAction {
                binding.incomingSamsungPane
                    .animate()
                    .alpha(0f)
                    .setDuration(REJECT_FADE_MS)
                    .withEndAction {
                        binding.incomingSamsungPane.alpha = 1f
                        flash.visibility = View.GONE
                        flash.scaleX = 1f
                        flash.scaleY = 1f
                        flash.alpha = 0f
                        button.scaleX = 1f
                        button.scaleY = 1f
                        then()
                    }
                    .start()
            }
            .start()
    }

    companion object {
        private const val TOUCH_DOWN_MS = 100L
        private const val TOUCH_UP_MS = 100L
        private const val TOUCH_PRESS_SCALE = 1.08f
        private const val ANSWER_BURST_MS = 120L
        private const val ANSWER_BURST_TARGET_SCALE = 2.8f
        private const val ANSWER_FLASH_EXPAND_MS = 75L
        private const val ANSWER_FLASH_EXPAND_SCALE = 10f
        private const val ANSWER_FLASH_FADE_MS = 55L
        private const val REJECT_FADE_MS = 200L

        fun hideVisualOverlays(binding: ActivityFakeIncomingCallBinding) {
            val views =
                listOf(
                    binding.samsungDeclineAura,
                    binding.samsungDeclineRipple1,
                    binding.samsungDeclineRipple2,
                    binding.samsungAnswerAura,
                    binding.samsungAnswerRipple1,
                    binding.samsungAnswerRipple2,
                    binding.samsungAnswerSwipeGlow,
                    binding.samsungDeclineSwipeGlow,
                    binding.samsungAnswerSuccessFlash,
                    binding.samsungRejectSuccessFlash,
                )
            views.forEach { v ->
                v.animate().cancel()
                v.visibility = View.GONE
                v.alpha = 0f
                v.scaleX = 1f
                v.scaleY = 1f
            }
        }
    }
}
