package dev.arpan.calling

import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Draws behind the system bars without the Android 15-deprecated
 * [android.view.Window.setStatusBarColor], [android.view.Window.setNavigationBarColor],
 * or [WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES].
 * Transparent bar colors stay in the theme so API 30–34 still paints them clear.
 */
internal fun ComponentActivity.enableDisplayEdgeToEdge(
    appearanceLightSystemBars: Boolean = !isNightMode(),
    enforceNavigationBarContrast: Boolean = true,
) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.attributes = window.attributes.apply {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
    if (Build.VERSION.SDK_INT < 35) {
        @Suppress("DEPRECATION")
        window.isStatusBarContrastEnforced = false
    }
    window.isNavigationBarContrastEnforced = enforceNavigationBarContrast
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = appearanceLightSystemBars
        isAppearanceLightNavigationBars = appearanceLightSystemBars
    }
}

internal fun View.applyDisplayEdgeToEdgePadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.systemBarsAndCutout()
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}

internal fun WindowInsetsCompat.systemBarsAndCutout() =
    getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

private fun ComponentActivity.isNightMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
