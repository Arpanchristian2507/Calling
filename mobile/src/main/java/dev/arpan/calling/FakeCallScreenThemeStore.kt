package dev.arpan.calling

import android.content.Context

/**
 * User preferences for the full-screen fake incoming / active call appearance.
 * Incoming OEM chrome: Samsung variants vs OnePlus, selected from the in-app themes gallery.
 */
object FakeCallScreenThemeStore {
    private const val PREFS = "dev.arpan.calling.fake_call_screen_theme"
    private const val KEY_BACKGROUND = "background_style"
    private const val KEY_LAYOUT = "layout_style"
    private const val KEY_INCOMING_UI_BRAND = "incoming_ui_brand"
    /** Legacy boolean from when Samsung visuals were toggled separately; removed after [migrateIncomingBrandV2]. */
    private const val KEY_SAMSUNG_INCOMING_VISUAL_EFFECTS = "samsung_incoming_visual_effects"
    private const val KEY_INCOMING_BRAND_MIGRATED_V2 = "incoming_brand_migrated_v2"

    /** Which OEM-style incoming call chrome to show. Active call screen is shared for now. */
    enum class IncomingCallUiBrand {
        /** Samsung pane + outward drag to answer/decline; One UI success flashes when enabled. */
        SAMSUNG_ONE_UI,

        /** Same Samsung pane and drag gesture; no optional success-flash styling. */
        SAMSUNG_SWIPE_UP,

        ONEPLUS,
    }

    enum class CallBackgroundStyle {
        /** Samsung-style default: subtle animated gradient behind controls. */
        MOVING_GRADIENT,

        /** Samsung-style: full-screen image from gallery (separate from caller avatar). */
        CUSTOM_GALLERY,

        /** Samsung-style: static dark backdrop. */
        DARK,

        /** Full-bleed caller photo backdrop when a caller photo is set. */
        CONTACT_PHOTO_FOCUS,
    }

    enum class CallLayoutStyle {
        STANDARD,
        COMPACT,
    }

    fun getBackgroundStyle(context: Context): CallBackgroundStyle {
        val raw = prefs(context).getString(KEY_BACKGROUND, null) ?: return CallBackgroundStyle.MOVING_GRADIENT
        return runCatching { CallBackgroundStyle.valueOf(raw) }.getOrDefault(CallBackgroundStyle.MOVING_GRADIENT)
    }

    fun setBackgroundStyle(context: Context, style: CallBackgroundStyle) {
        prefs(context).edit().putString(KEY_BACKGROUND, style.name).apply()
    }

    fun getLayoutStyle(context: Context): CallLayoutStyle {
        val raw = prefs(context).getString(KEY_LAYOUT, null) ?: return CallLayoutStyle.STANDARD
        return runCatching { CallLayoutStyle.valueOf(raw) }.getOrDefault(CallLayoutStyle.STANDARD)
    }

    fun setLayoutStyle(context: Context, style: CallLayoutStyle) {
        prefs(context).edit().putString(KEY_LAYOUT, style.name).apply()
    }

    fun getIncomingUiBrand(context: Context): IncomingCallUiBrand {
        migrateIncomingBrandV2(context)
        val p = prefs(context)
        val raw = p.getString(KEY_INCOMING_UI_BRAND, null) ?: return IncomingCallUiBrand.SAMSUNG_ONE_UI
        if (raw == "PIXEL") return IncomingCallUiBrand.ONEPLUS
        return runCatching { IncomingCallUiBrand.valueOf(raw) }.getOrDefault(IncomingCallUiBrand.SAMSUNG_ONE_UI)
    }

    fun setIncomingUiBrand(context: Context, brand: IncomingCallUiBrand) {
        prefs(context).edit().putString(KEY_INCOMING_UI_BRAND, brand.name).apply()
    }

    /** True for both Samsung incoming variants (shared layout + outward-drag gesture). */
    fun isSamsungIncomingFamily(brand: IncomingCallUiBrand): Boolean =
        brand == IncomingCallUiBrand.SAMSUNG_ONE_UI || brand == IncomingCallUiBrand.SAMSUNG_SWIPE_UP

    /**
     * Maps legacy stored value `SAMSUNG` plus optional [KEY_SAMSUNG_INCOMING_VISUAL_EFFECTS] into
     * [IncomingCallUiBrand.SAMSUNG_ONE_UI] vs [IncomingCallUiBrand.SAMSUNG_SWIPE_UP], then removes the old key.
     */
    private fun migrateIncomingBrandV2(context: Context) {
        val p = prefs(context)
        if (p.getBoolean(KEY_INCOMING_BRAND_MIGRATED_V2, false)) return
        val editor = p.edit().putBoolean(KEY_INCOMING_BRAND_MIGRATED_V2, true)
        val raw = p.getString(KEY_INCOMING_UI_BRAND, null)
        if (raw == null || raw == "SAMSUNG") {
            val effectsOn =
                if (p.contains(KEY_SAMSUNG_INCOMING_VISUAL_EFFECTS)) {
                    p.getBoolean(KEY_SAMSUNG_INCOMING_VISUAL_EFFECTS, true)
                } else {
                    true
                }
            editor.putString(
                KEY_INCOMING_UI_BRAND,
                if (effectsOn) {
                    IncomingCallUiBrand.SAMSUNG_ONE_UI.name
                } else {
                    IncomingCallUiBrand.SAMSUNG_SWIPE_UP.name
                },
            )
        }
        if (p.contains(KEY_SAMSUNG_INCOMING_VISUAL_EFFECTS)) {
            editor.remove(KEY_SAMSUNG_INCOMING_VISUAL_EFFECTS)
        }
        editor.apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
