package dev.arpan.calling

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import dev.arpan.calling.databinding.ActivityCallThemesBinding

class CallThemesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallThemesBinding
    private lateinit var adapter: CallThemesAdapter

    private val samsungThemes =
        listOf(
            CallThemesAdapter.ThemeCardItem(
                R.string.theme_card_samsung_one_ui,
                FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_ONE_UI,
                R.drawable.mini_preview_samsung_one_ui,
            ),
            CallThemesAdapter.ThemeCardItem(
                R.string.theme_card_samsung_swipe_up,
                FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_SWIPE_UP,
                R.drawable.mini_preview_samsung_swipe_up,
            ),
        )

    private val onePlusThemes =
        listOf(
            CallThemesAdapter.ThemeCardItem(
                R.string.theme_card_oneplus_oxygen,
                FakeCallScreenThemeStore.IncomingCallUiBrand.ONEPLUS,
                R.drawable.mini_preview_oneplus,
            ),
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCallThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        val initial = FakeCallScreenThemeStore.getIncomingUiBrand(this)
        val onSamsungSide = FakeCallScreenThemeStore.isSamsungIncomingFamily(initial)
        adapter =
            CallThemesAdapter(
                items = if (onSamsungSide) samsungThemes else onePlusThemes,
                selectedBrand = initial,
                onIconAction = { action ->
                    val msg =
                        when (action) {
                            CallThemesAdapter.ThemeIconAction.PREVIEW ->
                                getString(R.string.themes_icon_preview_stub)
                            CallThemesAdapter.ThemeIconAction.EDIT ->
                                getString(R.string.themes_icon_edit_stub)
                        }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                },
            )

        binding.themeRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.themeRecycler.adapter = adapter

        val startTab = if (onSamsungSide) 0 else 1
        binding.themeTabs.getTabAt(startTab)?.select()
        adapter.submitList(if (startTab == 0) samsungThemes else onePlusThemes)

        binding.themeTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> {
                            if (!FakeCallScreenThemeStore.isSamsungIncomingFamily(adapter.selectedBrand())) {
                                adapter.setSelectedBrand(FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_ONE_UI)
                            }
                            adapter.submitList(samsungThemes)
                        }
                        1 -> {
                            if (adapter.selectedBrand() != FakeCallScreenThemeStore.IncomingCallUiBrand.ONEPLUS) {
                                adapter.setSelectedBrand(FakeCallScreenThemeStore.IncomingCallUiBrand.ONEPLUS)
                            }
                            adapter.submitList(onePlusThemes)
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )

        binding.fabApplyTheme.setOnClickListener {
            FakeCallScreenThemeStore.setIncomingUiBrand(this, adapter.selectedBrand())
            Toast.makeText(this, R.string.themes_applied, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
