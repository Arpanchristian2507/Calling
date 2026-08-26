package dev.arpan.calling

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.arpan.calling.databinding.ActivityActiveCallBinding
import dev.arpan.calling.databinding.ActivityFakeIncomingCallBinding
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

class FakeIncomingCallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFakeIncomingCallBinding
    private var samsungIncomingCallTheme: SamsungIncomingCallTheme? = null

    /** Optional One UI visuals; pickup uses [attachSamsungOmnidirectionalSwipePickup] and is unchanged when this is [SamsungIncomingCallTheme.GestureVisualSink.NONE]. */
    private var samsungGestureVisualSink: SamsungIncomingCallTheme.GestureVisualSink =
        SamsungIncomingCallTheme.GestureVisualSink.NONE

    private var activeCallBinding: ActivityActiveCallBinding? = null
    private val timerHandler = Handler(Looper.getMainLooper())
    private var callStartElapsed: Long = 0L
    private var isCallAnswered: Boolean = false
    private var speakerRouteOn: Boolean = false

    private val slideTouchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var samsungIdleBreathing: AnimatorSet? = null
    private var samsungSwipeSession: SamsungSwipeSession? = null
    private var samsungRippleSpringAnimator: ValueAnimator? = null

    private var opKnobDownRawY = 0f
    private var opKnobStartTranslationY = 0f
    private var opKnobAccumulatedDy = 0f
    private var opKnobExceededTouchSlop = false
    private val opMaxSlidePx by lazy { 200f * resources.displayMetrics.density }

    private val timerRunnable =
        object : Runnable {
            override fun run() {
                updateCallTimer()
                timerHandler.postDelayed(this, ONE_SECOND_MS)
            }
        }

    private fun incomingBrand(): FakeCallScreenThemeStore.IncomingCallUiBrand =
        FakeCallScreenThemeStore.getIncomingUiBrand(this)

    private fun useSamsungIncomingUi(): Boolean =
        FakeCallScreenThemeStore.isSamsungIncomingFamily(incomingBrand())

    private fun useSamsungOneUiIncomingVisuals(): Boolean =
        incomingBrand() == FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_ONE_UI

    private fun useOnePlusIncomingUi(): Boolean =
        incomingBrand() == FakeCallScreenThemeStore.IncomingCallUiBrand.ONEPLUS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FakeCallNotifier.cancel(this)
        applyPersonalIncomingWindowFlags()

        binding = ActivityFakeIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.callScreensRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val caller = intent.getStringExtra(EXTRA_CALLER) ?: getString(R.string.fake_call_default_name)
        setCallerDisplayName(caller)
        showIncomingState()
        wireIncomingActions()
    }

    private fun setCallerDisplayName(name: CharSequence) {
        binding.samsungCallerName.text = name
        binding.onePlusCallerName.text = name
    }

    private fun callerDisplayName(): CharSequence =
        if (useSamsungIncomingUi()) {
            binding.samsungCallerName.text
        } else {
            binding.onePlusCallerName.text
        }

    private fun wireIncomingActions() {
        val endIncoming: () -> Unit = {
            FakeCallRinger.stop(applicationContext)
            finish()
        }

        if (useSamsungIncomingUi()) {
            installSamsungHoldSwipeHandlers()
            val sendStub: () -> Unit = {
                Toast.makeText(this, R.string.fake_call_samsung_send_message_stub, Toast.LENGTH_SHORT).show()
            }
            binding.samsungSendMessage.setOnClickListener { sendStub() }
            binding.samsungSendMessagePill.setOnClickListener { sendStub() }
        } else if (useOnePlusIncomingUi()) {
            installOnePlusAnswerKnob(endIncoming)
            binding.onePlusReplyPill.setOnClickListener {
                Toast.makeText(this, R.string.fake_call_oneplus_reply_stub, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun installSamsungHoldSwipeHandlers() {
        attachSamsungOmnidirectionalSwipePickup(
            button = binding.samsungAnswerButton,
            ripple1 = binding.samsungAnswerRipple1,
            ripple2 = binding.samsungAnswerRipple2,
            aura = binding.samsungAnswerAura,
            swipeGlow = binding.samsungAnswerSwipeGlow,
            isAnswer = true,
            onSuccess = { answerCall() },
        )
        attachSamsungOmnidirectionalSwipePickup(
            button = binding.samsungDeclineButton,
            ripple1 = binding.samsungDeclineRipple1,
            ripple2 = binding.samsungDeclineRipple2,
            aura = binding.samsungDeclineAura,
            swipeGlow = binding.samsungDeclineSwipeGlow,
            isAnswer = false,
            onSuccess = {
                FakeCallRinger.stop(applicationContext)
                finish()
            },
        )
    }

    private fun installOnePlusAnswerKnob(onReject: () -> Unit) {
        binding.onePlusAnswerKnob.bindTouchListener(
            OnePlusAnswerKnobTouchListener(onReject = onReject),
        )
    }

    private inner class OnePlusAnswerKnobTouchListener(
        private val onReject: () -> Unit,
    ) : View.OnTouchListener {
        override fun onTouch(
            v: View,
            event: MotionEvent,
        ): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    opKnobDownRawY = event.rawY
                    opKnobStartTranslationY = v.translationY
                    opKnobAccumulatedDy = 0f
                    opKnobExceededTouchSlop = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - opKnobDownRawY
                    opKnobAccumulatedDy = dy
                    if (abs(dy) >= slideTouchSlop) {
                        opKnobExceededTouchSlop = true
                    }
                    if (abs(opKnobAccumulatedDy) < slideTouchSlop && v.translationY == opKnobStartTranslationY) {
                        return true
                    }
                    val next = (opKnobStartTranslationY + dy).coerceIn(-opMaxSlidePx, opMaxSlidePx)
                    v.translationY = next
                    return true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    if (event.actionMasked == MotionEvent.ACTION_UP && !opKnobExceededTouchSlop) {
                        v.performClick()
                    }
                    val thresh = opMaxSlidePx * ONEPLUS_SLIDE_COMMIT_RATIO
                    val ty = v.translationY
                    when {
                        ty < -thresh -> {
                            v.animate().translationY(0f).setDuration(150L).withEndAction { answerCall() }.start()
                        }
                        ty > thresh -> {
                            v.animate().translationY(0f).setDuration(150L).withEndAction { onReject() }.start()
                        }
                        else -> {
                            v.animate().translationY(0f).setDuration(200L).start()
                        }
                    }
                    return true
                }
                else -> return false
            }
        }
    }

    private data class SamsungSwipeSession(
        val button: View,
        val ripple1: View,
        val ripple2: View,
        val aura: View,
        val swipeGlow: View,
        val isAnswer: Boolean,
        val startRawX: Float,
        val startRawY: Float,
        val maxSwipePx: Float,
        val onSuccess: () -> Unit,
        var exceededTouchSlop: Boolean = false,
    )

    /**
     * Samsung incoming: drag answer or decline **away from rest** in any direction. Travel magnitude is capped by
     * [resolveSamsungMaxSwipePxForButton] (distance from [R.id.samsungSwipeTargetGuideline] to the action, with a px
     * floor). Progress drives ripples, aura, and glow; commit on [MotionEvent.ACTION_UP] when progress ≥
     * [SAMSUNG_SWIPE_COMMIT_RATIO].
     */
    private fun attachSamsungOmnidirectionalSwipePickup(
        button: AccessibleTouchFrameLayout,
        ripple1: View,
        ripple2: View,
        aura: View,
        swipeGlow: View,
        isAnswer: Boolean,
        onSuccess: () -> Unit,
    ) {
        button.bindTouchListener(
            SamsungSwipeTouchListener(
                ripple1 = ripple1,
                ripple2 = ripple2,
                aura = aura,
                swipeGlow = swipeGlow,
                isAnswer = isAnswer,
                onSuccess = onSuccess,
            ),
        )
    }

    private inner class SamsungSwipeTouchListener(
        private val ripple1: View,
        private val ripple2: View,
        private val aura: View,
        private val swipeGlow: View,
        private val isAnswer: Boolean,
        private val onSuccess: () -> Unit,
    ) : View.OnTouchListener {
        override fun onTouch(
            v: View,
            event: MotionEvent,
        ): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopSamsungIdleBreathing()
                    abortSamsungSwipeSessionIfOtherButton(v)
                    cancelSamsungRippleSpringAnimator()
                    v.animate().cancel()
                    samsungGestureVisualSink.onTouchDown(v)
                    clearSamsungRippleViews(ripple1, ripple2, aura)
                    swipeGlow.visibility = View.GONE
                    swipeGlow.alpha = 0f
                    swipeGlow.scaleX = 1f
                    swipeGlow.scaleY = 1f
                    v.translationX = 0f
                    v.translationY = 0f
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    val maxPx = resolveSamsungMaxSwipePxForButton(v)
                    ensureSamsungRipplePivots(ripple1, ripple2, aura, swipeGlow)
                    samsungSwipeSession =
                        SamsungSwipeSession(
                            button = v,
                            ripple1 = ripple1,
                            ripple2 = ripple2,
                            aura = aura,
                            swipeGlow = swipeGlow,
                            isAnswer = isAnswer,
                            startRawX = event.rawX,
                            startRawY = event.rawY,
                            maxSwipePx = maxPx,
                            onSuccess = onSuccess,
                        )
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val s = samsungSwipeSession
                    if (s == null || s.button != v) return false
                    val dx = event.rawX - s.startRawX
                    val dy = event.rawY - s.startRawY
                    val dist = hypot(dx, dy)
                    if (dist >= slideTouchSlop) {
                        s.exceededTouchSlop = true
                    }
                    if (dist < slideTouchSlop && v.translationX == 0f && v.translationY == 0f) {
                        return true
                    }
                    v.animate().cancel()
                    val travel = (dist - slideTouchSlop).coerceIn(0f, s.maxSwipePx)
                    val p = (travel / s.maxSwipePx).coerceIn(0f, 1f)
                    if (dist < 0.001f) {
                        return true
                    }
                    val ux = dx / dist
                    val uy = dy / dist
                    v.translationX = ux * travel
                    v.translationY = uy * travel
                    applySamsungRippleProgress(p, s.ripple1, s.ripple2, s.aura)
                    if (p > 0.02f) {
                        s.swipeGlow.visibility = View.VISIBLE
                        if (s.swipeGlow.width > 0) {
                            s.swipeGlow.pivotX = s.swipeGlow.width / 2f
                            s.swipeGlow.pivotY = s.swipeGlow.height / 2f
                        }
                        val glowScale = 1f + SAMSUNG_SWIPE_GLOW_EXTRA_SCALE * p
                        s.swipeGlow.scaleX = glowScale
                        s.swipeGlow.scaleY = glowScale
                        s.swipeGlow.alpha = p * SAMSUNG_SWIPE_GLOW_MAX_ALPHA
                    } else {
                        s.swipeGlow.visibility = View.GONE
                        s.swipeGlow.alpha = 0f
                    }
                    val scaleBump = SAMSUNG_TOUCH_BASE_SCALE + SAMSUNG_DRAG_EXTRA_SCALE * p
                    v.scaleX = scaleBump
                    v.scaleY = scaleBump
                    return true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    val s = samsungSwipeSession
                    if (s == null || s.button != v) {
                        samsungGestureVisualSink.onTouchUp(v)
                        return true
                    }
                    if (event.actionMasked == MotionEvent.ACTION_UP && !s.exceededTouchSlop) {
                        v.performClick()
                    }
                    samsungSwipeSession = null
                    val p = samsungDragProgressFromTranslations(v, s.maxSwipePx)
                    if (p >= SAMSUNG_SWIPE_COMMIT_RATIO) {
                        // Do not call onTouchUp here: it animates scale → 1 and fights the success burst (feels laggy).
                        v.animate().cancel()
                        v.translationX = 0f
                        v.translationY = 0f
                        clearSamsungRippleViews(s.ripple1, s.ripple2, s.aura)
                        s.swipeGlow.visibility = View.GONE
                        samsungGestureVisualSink.onSwipeSucceeded(s.isAnswer, v, s.onSuccess)
                    } else {
                        samsungGestureVisualSink.onTouchUp(v)
                        springBackSamsungSwipe(s)
                    }
                    return true
                }
                else -> return false
            }
        }
    }

    private fun samsungDragProgressFromTranslations(
        v: View,
        maxSwipePx: Float,
    ): Float {
        if (maxSwipePx <= 0f) return 0f
        val mag = hypot(v.translationX, v.translationY)
        return (mag / maxSwipePx).coerceIn(0f, 1f)
    }

    private fun resolveSamsungMaxSwipePxForButton(button: View): Float {
        val pane = binding.incomingSamsungPane
        val paneH = pane.height
        val fallback = SAMSUNG_SWIPE_FALLBACK_PX * resources.displayMetrics.density
        if (paneH <= 0 || button.height <= 0) return fallback
        val guidelineY = paneH * SAMSUNG_SWIPE_GUIDELINE_FRACTION
        val span = button.y - guidelineY
        return maxOf(span.coerceAtLeast(0f), fallback)
    }

    private fun applySamsungRippleProgress(
        progress: Float,
        ripple1: View,
        ripple2: View,
        aura: View,
    ) {
        val p = progress.coerceIn(0f, 1f)
        val p2 = ((p - SAMSUNG_RIPPLE2_LAG) / (1f - SAMSUNG_RIPPLE2_LAG)).coerceIn(0f, 1f)

        fun styleRipple(ripple: View, pr: Float) {
            ripple.visibility = View.VISIBLE
            val scale = SAMSUNG_RIPPLE_MIN_SCALE + (SAMSUNG_RIPPLE_MAX_SCALE - SAMSUNG_RIPPLE_MIN_SCALE) * pr
            ripple.scaleX = scale
            ripple.scaleY = scale
            ripple.alpha = SAMSUNG_RIPPLE_MIN_ALPHA + (SAMSUNG_RIPPLE_MAX_ALPHA - SAMSUNG_RIPPLE_MIN_ALPHA) * pr
        }

        styleRipple(ripple1, p)
        if (p2 <= 0f) {
            ripple2.visibility = View.GONE
            ripple2.alpha = 0f
            ripple2.scaleX = SAMSUNG_RIPPLE_MIN_SCALE
            ripple2.scaleY = SAMSUNG_RIPPLE_MIN_SCALE
        } else {
            styleRipple(ripple2, p2)
        }

        if (p > 0.02f) {
            aura.visibility = View.VISIBLE
            aura.scaleX = 1f + 0.35f * p
            aura.scaleY = 1f + 0.35f * p
            aura.alpha = p * SAMSUNG_AURA_MAX_ALPHA
        } else {
            aura.visibility = View.GONE
            aura.alpha = 0f
        }
    }

    private fun clearSamsungRippleViews(
        ripple1: View,
        ripple2: View,
        aura: View,
    ) {
        listOf(ripple1, ripple2, aura).forEach { v ->
            v.animate().cancel()
            v.visibility = View.GONE
            v.alpha = 0f
            v.scaleX = 1f
            v.scaleY = 1f
        }
    }

    private fun ensureSamsungRipplePivots(
        ripple1: View,
        ripple2: View,
        aura: View,
        swipeGlow: View,
    ) {
        listOf(ripple1, ripple2, aura, swipeGlow).forEach { v ->
            if (v.width > 0) {
                v.pivotX = v.width / 2f
                v.pivotY = v.height / 2f
            }
        }
    }

    private fun resetSamsungSwipeSessionVisuals(s: SamsungSwipeSession) {
        s.button.animate().cancel()
        s.button.translationX = 0f
        s.button.translationY = 0f
        s.button.scaleX = 1f
        s.button.scaleY = 1f
        s.swipeGlow.animate().cancel()
        s.swipeGlow.visibility = View.GONE
        s.swipeGlow.alpha = 0f
        s.swipeGlow.scaleX = 1f
        s.swipeGlow.scaleY = 1f
        clearSamsungRippleViews(s.ripple1, s.ripple2, s.aura)
    }

    private fun abortSamsungSwipeSessionIfOtherButton(pressed: View) {
        val s = samsungSwipeSession ?: return
        if (s.button != pressed) {
            samsungGestureVisualSink.onTouchUp(s.button)
            resetSamsungSwipeSessionVisuals(s)
            samsungSwipeSession = null
        }
    }

    private fun springBackSamsungSwipe(s: SamsungSwipeSession) {
        cancelSamsungRippleSpringAnimator()
        val startP = samsungDragProgressFromTranslations(s.button, s.maxSwipePx)
        s.button
            .animate()
            .translationX(0f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(SAMSUNG_SPRING_BACK_MS)
            .start()
        s.swipeGlow
            .animate()
            .alpha(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(SAMSUNG_SPRING_BACK_MS)
            .withEndAction {
                s.swipeGlow.visibility = View.GONE
            }
            .start()
        val spring =
            ValueAnimator.ofFloat(startP, 0f).apply {
                duration = SAMSUNG_SPRING_BACK_MS
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener { a ->
                    applySamsungRippleProgress(a.animatedValue as Float, s.ripple1, s.ripple2, s.aura)
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            samsungRippleSpringAnimator = null
                            clearSamsungRippleViews(s.ripple1, s.ripple2, s.aura)
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            samsungRippleSpringAnimator = null
                            clearSamsungRippleViews(s.ripple1, s.ripple2, s.aura)
                        }
                    },
                )
            }
        samsungRippleSpringAnimator = spring
        spring.start()
    }

    private fun cancelSamsungRippleSpringAnimator() {
        samsungRippleSpringAnimator?.cancel()
        samsungRippleSpringAnimator = null
    }

    private fun cancelSamsungSwipeGestureState() {
        cancelSamsungRippleSpringAnimator()
        samsungSwipeSession?.let { s ->
            samsungGestureVisualSink.onTouchUp(s.button)
            resetSamsungSwipeSessionVisuals(s)
        }
        samsungSwipeSession = null
    }

    private fun stopSamsungIdleBreathing() {
        samsungIdleBreathing?.cancel()
        samsungIdleBreathing = null
        if (::binding.isInitialized && useSamsungIncomingUi()) {
            binding.samsungAnswerButton.animate().cancel()
            binding.samsungDeclineButton.animate().cancel()
            binding.samsungAnswerButton.scaleX = 1f
            binding.samsungAnswerButton.scaleY = 1f
            binding.samsungDeclineButton.scaleX = 1f
            binding.samsungDeclineButton.scaleY = 1f
        }
    }

    private fun startSamsungIdleBreathingIfNeeded() {
        if (!::binding.isInitialized || !useSamsungIncomingUi() || isCallAnswered) return
        stopSamsungIdleBreathing()
        val a = binding.samsungAnswerButton
        val d = binding.samsungDeclineButton
        val breathA =
            ObjectAnimator.ofPropertyValuesHolder(
                a,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, SAMSUNG_IDLE_BREATH_MAX_SCALE),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, SAMSUNG_IDLE_BREATH_MAX_SCALE),
            ).apply {
                duration = SAMSUNG_IDLE_BREATH_HALF_PERIOD_MS
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
            }
        val breathD =
            ObjectAnimator.ofPropertyValuesHolder(
                d,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, SAMSUNG_IDLE_BREATH_MAX_SCALE),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, SAMSUNG_IDLE_BREATH_MAX_SCALE),
            ).apply {
                duration = SAMSUNG_IDLE_BREATH_HALF_PERIOD_MS
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
            }
        samsungIdleBreathing =
            AnimatorSet().apply {
                playTogether(breathA, breathD)
                start()
            }
    }

    private fun refreshCallScreenChrome() {
        CallScreenStyleApplier.apply(binding, activeCallBinding)
    }

    private fun applyIncomingPaneVisibility() {
        binding.incomingSamsungPane.visibility =
            if (useSamsungIncomingUi()) View.VISIBLE else View.GONE
        binding.incomingOnePlusPane.visibility =
            if (useOnePlusIncomingUi()) View.VISIBLE else View.GONE
    }

    override fun onStart() {
        super.onStart()
        CallScreenStyleApplier.startRootBackgroundAnimationIfRunning(binding.callScreensRoot)
        if (isCallAnswered) {
            startTimer()
            FakeCallRinger.startAnsweredVoiceIfConfigured(this)
            applySpeakerButtonState()
        } else {
            FakeCallRinger.start(this)
        }
    }

    override fun onStop() {
        CallScreenStyleApplier.pauseRootBackgroundAnimation(binding.callScreensRoot)
        FakeCallRinger.stop(applicationContext)
        stopTimer()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val caller = intent.getStringExtra(EXTRA_CALLER) ?: getString(R.string.fake_call_default_name)
        if (::binding.isInitialized) {
            setCallerDisplayName(caller)
            activeCallBinding?.callerName?.text = caller
            speakerRouteOn = false
            showIncomingState()
        }
        FakeCallRinger.start(this)
    }

    private fun answerCall() {
        FakeCallRinger.stopIncomingRing()
        showAnsweredState()
        FakeCallRinger.startAnsweredVoiceIfConfigured(this)
    }

    private fun ensureActiveCallBinding(): ActivityActiveCallBinding {
        activeCallBinding?.let { return it }
        val inflated =
            ActivityActiveCallBinding.inflate(
                LayoutInflater.from(this),
                binding.callScreensRoot,
                true,
            )
        inflated.endCallButton.setOnClickListener {
            finish()
        }
        inflated.speakerButton.setOnClickListener {
            speakerRouteOn = !speakerRouteOn
            inflated.speakerButton.isChecked = speakerRouteOn
            FakeCallRinger.setAnsweredSpeakerphoneEnabled(this, speakerRouteOn)
        }
        inflated.root.visibility = View.GONE
        activeCallBinding = inflated
        return inflated
    }

    private fun applySpeakerButtonState() {
        val active = activeCallBinding ?: return
        active.speakerButton.isChecked = speakerRouteOn
    }

    private fun showIncomingState() {
        stopSamsungIdleBreathing()
        cancelSamsungSwipeGestureState()
        samsungIncomingCallTheme?.stop()
        samsungIncomingCallTheme = null
        samsungGestureVisualSink = SamsungIncomingCallTheme.GestureVisualSink.NONE

        applyIncomingPaneVisibility()
        refreshCallScreenChrome()
        isCallAnswered = false
        speakerRouteOn = false
        stopTimer()
        activeCallBinding?.root?.visibility = View.GONE
        binding.incomingCallRoot.visibility = View.VISIBLE

        binding.samsungAnswerButton.translationX = 0f
        binding.samsungAnswerButton.translationY = 0f
        binding.samsungDeclineButton.translationX = 0f
        binding.samsungDeclineButton.translationY = 0f
        binding.onePlusAnswerKnob.translationY = 0f

        if (useSamsungIncomingUi()) {
            resetSamsungIncomingVisualStateToDefaults()
            if (useSamsungOneUiIncomingVisuals()) {
                val theme = SamsungIncomingCallTheme(this, binding)
                samsungIncomingCallTheme = theme
                samsungGestureVisualSink = theme.gestureVisualSink
                theme.start()
            } else {
                samsungIncomingCallTheme = null
                samsungGestureVisualSink = SamsungIncomingCallTheme.GestureVisualSink.NONE
                SamsungIncomingCallTheme.hideVisualOverlays(binding)
            }
        }

        applyCallerAvatars()
        if (useSamsungIncomingUi()) {
            binding.incomingSamsungPane.post { startSamsungIdleBreathingIfNeeded() }
        }
    }

    private fun showAnsweredState() {
        stopSamsungIdleBreathing()
        cancelSamsungSwipeGestureState()
        isCallAnswered = true
        callStartElapsed = SystemClock.elapsedRealtime()

        val active = ensureActiveCallBinding()
        binding.incomingCallRoot.visibility = View.GONE
        active.root.visibility = View.VISIBLE

        active.onCallLabel.text = getString(R.string.fake_call_connected_label)
        active.callerName.text = callerDisplayName()
        active.callerNumber.visibility = View.GONE
        active.callTimer.text = getString(R.string.fake_call_connected_timer)

        active.speakerButton.isChecked = speakerRouteOn
        refreshCallScreenChrome()
        applyCallerAvatars()
        startTimer()
    }

    private fun applyCallerAvatars() {
        CallerAvatarStore.apply(
            this,
            binding.samsungCallerAvatar,
            CallerAvatarStore.AvatarStyle.INCOMING_SCREEN,
        )
        activeCallBinding?.let { active ->
            CallerAvatarStore.apply(
                this,
                active.callerAvatar,
                CallerAvatarStore.AvatarStyle.ACTIVE_SCREEN,
            )
        }
    }

    private fun resetSamsungIncomingVisualStateToDefaults() {
        val resetViews =
            listOf(
                binding.samsungHdRow,
                binding.samsungCallerName,
                binding.samsungCallerSubtitle,
                binding.samsungCallerAvatarCard,
                binding.samsungCallerAvatar,
                binding.samsungDeclineButton,
                binding.samsungAnswerButton,
            )
        resetViews.forEach { v ->
            v.animate().cancel()
            v.alpha = 1f
            v.translationX = 0f
            v.translationY = 0f
            v.scaleX = 1f
            v.scaleY = 1f
        }
        listOf(
            binding.samsungAnswerRipple1,
            binding.samsungAnswerRipple2,
            binding.samsungDeclineRipple1,
            binding.samsungDeclineRipple2,
            binding.samsungAnswerAura,
            binding.samsungDeclineAura,
        ).forEach { v ->
            v.animate().cancel()
            v.visibility = View.GONE
            v.alpha = 0f
            v.scaleX = 1f
            v.scaleY = 1f
        }
        binding.samsungAnswerSwipeGlow.animate().cancel()
        binding.samsungDeclineSwipeGlow.animate().cancel()
        binding.samsungAnswerSwipeGlow.alpha = 0f
        binding.samsungDeclineSwipeGlow.alpha = 0f
        binding.samsungAnswerSwipeGlow.scaleX = 1f
        binding.samsungAnswerSwipeGlow.scaleY = 1f
        binding.samsungDeclineSwipeGlow.scaleX = 1f
        binding.samsungDeclineSwipeGlow.scaleY = 1f
        binding.samsungAnswerSwipeGlow.visibility = View.GONE
        binding.samsungDeclineSwipeGlow.visibility = View.GONE
        binding.samsungAnswerSuccessFlash.animate().cancel()
        binding.samsungRejectSuccessFlash.animate().cancel()
        binding.samsungAnswerSuccessFlash.visibility = View.GONE
        binding.samsungRejectSuccessFlash.visibility = View.GONE
        binding.samsungAnswerSuccessFlash.alpha = 0f
        binding.samsungRejectSuccessFlash.alpha = 0f
        binding.incomingSamsungPane.alpha = 1f
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
        activeCallBinding?.callTimer?.text =
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        stopSamsungIdleBreathing()
        cancelSamsungSwipeGestureState()
        samsungIncomingCallTheme?.stop()
        samsungIncomingCallTheme = null
        samsungGestureVisualSink = SamsungIncomingCallTheme.GestureVisualSink.NONE
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CALLER: String = "extra_caller"
        const val EXTRA_SCHEDULE_ID: String = "extra_schedule_id"
        private const val ONE_SECOND_MS: Long = 1000L
        /** Minimum drag distance (px) when the incoming pane is not measured yet. */
        private const val SAMSUNG_SWIPE_FALLBACK_PX: Float = 140f
        /** Must match [R.id.samsungSwipeTargetGuideline] `layout_constraintGuide_percent`. */
        private const val SAMSUNG_SWIPE_GUIDELINE_FRACTION: Float = 0.42f
        /** Release commit threshold: fraction of max swipe toward the guideline (0–1). */
        private const val SAMSUNG_SWIPE_COMMIT_RATIO: Float = 0.72f
        private const val SAMSUNG_SPRING_BACK_MS: Long = 220L
        private const val SAMSUNG_SWIPE_GLOW_MAX_ALPHA: Float = 0.62f
        private const val SAMSUNG_SWIPE_GLOW_EXTRA_SCALE: Float = 0.48f
        /** Finger-down base scale while swiping (matches One UI press spec ~1.08). */
        private const val SAMSUNG_TOUCH_BASE_SCALE: Float = 1.08f
        private const val SAMSUNG_DRAG_EXTRA_SCALE: Float = 0.07f
        private const val SAMSUNG_IDLE_BREATH_MAX_SCALE: Float = 1.035f
        private const val SAMSUNG_IDLE_BREATH_HALF_PERIOD_MS: Long = 1100L
        private const val SAMSUNG_RIPPLE_MIN_SCALE: Float = 0.38f
        private const val SAMSUNG_RIPPLE_MAX_SCALE: Float = 2.15f
        private const val SAMSUNG_RIPPLE_MIN_ALPHA: Float = 0.08f
        private const val SAMSUNG_RIPPLE_MAX_ALPHA: Float = 0.58f
        private const val SAMSUNG_AURA_MAX_ALPHA: Float = 0.16f
        /** Second ring starts after this fraction of the hold (0–1). */
        private const val SAMSUNG_RIPPLE2_LAG: Float = 0.07f
        private const val ONEPLUS_SLIDE_COMMIT_RATIO: Float = 0.38f
    }
}
