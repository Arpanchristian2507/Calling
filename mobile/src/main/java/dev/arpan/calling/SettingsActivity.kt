package dev.arpan.calling

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.arpan.calling.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private val pickCallBackground =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@registerForActivityResult
            if (CallBackgroundImageStore.saveFromGalleryUri(this, uri)) {
                FakeCallScreenThemeStore.setBackgroundStyle(
                    this,
                    FakeCallScreenThemeStore.CallBackgroundStyle.CUSTOM_GALLERY,
                )
                toast(getString(R.string.settings_call_background_saved))
            } else {
                toast(getString(R.string.settings_call_background_failed))
            }
            syncUiFromPrefs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.openCallThemes.setOnClickListener {
            startActivity(Intent(this, CallThemesActivity::class.java))
        }

        binding.pickCallBackground.setOnClickListener {
            pickCallBackground.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }

        binding.clearCallBackground.setOnClickListener {
            CallBackgroundImageStore.clear(this)
            if (FakeCallScreenThemeStore.getBackgroundStyle(this) ==
                FakeCallScreenThemeStore.CallBackgroundStyle.CUSTOM_GALLERY
            ) {
                FakeCallScreenThemeStore.setBackgroundStyle(
                    this,
                    FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT,
                )
            }
            toast(getString(R.string.settings_call_background_cleared))
            syncUiFromPrefs()
        }

        binding.openPrivacyPolicy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url))))
        }

        syncUiFromPrefs()
    }

    private fun syncUiFromPrefs() {
        cohereBackgroundPrefsWithAssets()

        binding.backgroundStyleGroup.setOnCheckedChangeListener(null)
        when (FakeCallScreenThemeStore.getBackgroundStyle(this)) {
            FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT ->
                binding.backgroundStyleGroup.check(R.id.radio_bg_gradient)
            FakeCallScreenThemeStore.CallBackgroundStyle.CUSTOM_GALLERY ->
                binding.backgroundStyleGroup.check(R.id.radio_bg_custom)
            FakeCallScreenThemeStore.CallBackgroundStyle.DARK ->
                binding.backgroundStyleGroup.check(R.id.radio_bg_dark)
            FakeCallScreenThemeStore.CallBackgroundStyle.CONTACT_PHOTO_FOCUS ->
                binding.backgroundStyleGroup.check(R.id.radio_bg_contact)
        }
        binding.backgroundStyleGroup.setOnCheckedChangeListener { _, checkedId ->
            val style = backgroundStyleForRadioId(checkedId) ?: return@setOnCheckedChangeListener
            if (style == FakeCallScreenThemeStore.CallBackgroundStyle.CUSTOM_GALLERY &&
                !CallBackgroundImageStore.hasCustomBackground(this)
            ) {
                toast(getString(R.string.settings_theme_custom_first))
                syncUiFromPrefs()
                return@setOnCheckedChangeListener
            }
            if (style == FakeCallScreenThemeStore.CallBackgroundStyle.CONTACT_PHOTO_FOCUS &&
                !CallerAvatarStore.hasCustomAvatar(this)
            ) {
                toast(getString(R.string.settings_theme_contact_first))
                syncUiFromPrefs()
                return@setOnCheckedChangeListener
            }
            FakeCallScreenThemeStore.setBackgroundStyle(this, style)
        }

        binding.layoutStyleGroup.setOnCheckedChangeListener(null)
        when (FakeCallScreenThemeStore.getLayoutStyle(this)) {
            FakeCallScreenThemeStore.CallLayoutStyle.STANDARD ->
                binding.layoutStyleGroup.check(R.id.radio_layout_standard)
            FakeCallScreenThemeStore.CallLayoutStyle.COMPACT ->
                binding.layoutStyleGroup.check(R.id.radio_layout_compact)
        }
        binding.layoutStyleGroup.setOnCheckedChangeListener { _, checkedId ->
            val layout =
                when (checkedId) {
                    R.id.radio_layout_standard ->
                        FakeCallScreenThemeStore.CallLayoutStyle.STANDARD
                    R.id.radio_layout_compact ->
                        FakeCallScreenThemeStore.CallLayoutStyle.COMPACT
                    else -> return@setOnCheckedChangeListener
                }
            FakeCallScreenThemeStore.setLayoutStyle(this, layout)
        }

        binding.clearCallBackground.visibility =
            if (CallBackgroundImageStore.hasCustomBackground(this)) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun cohereBackgroundPrefsWithAssets() {
        var style = FakeCallScreenThemeStore.getBackgroundStyle(this)
        if (style == FakeCallScreenThemeStore.CallBackgroundStyle.CUSTOM_GALLERY &&
            !CallBackgroundImageStore.hasCustomBackground(this)
        ) {
            FakeCallScreenThemeStore.setBackgroundStyle(
                this,
                FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT,
            )
            style = FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT
        }
        if (style == FakeCallScreenThemeStore.CallBackgroundStyle.CONTACT_PHOTO_FOCUS &&
            !CallerAvatarStore.hasCustomAvatar(this)
        ) {
            FakeCallScreenThemeStore.setBackgroundStyle(
                this,
                FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT,
            )
        }
    }

    private fun backgroundStyleForRadioId(checkedId: Int): FakeCallScreenThemeStore.CallBackgroundStyle? =
        when (checkedId) {
            R.id.radio_bg_gradient -> FakeCallScreenThemeStore.CallBackgroundStyle.MOVING_GRADIENT
            R.id.radio_bg_custom -> FakeCallScreenThemeStore.CallBackgroundStyle.CUSTOM_GALLERY
            R.id.radio_bg_dark -> FakeCallScreenThemeStore.CallBackgroundStyle.DARK
            R.id.radio_bg_contact -> FakeCallScreenThemeStore.CallBackgroundStyle.CONTACT_PHOTO_FOCUS
            else -> null
        }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
