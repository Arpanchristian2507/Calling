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

    private val iPhoneThemes =
        listOf(
            CallThemesAdapter.ThemeCardItem(
                R.string.theme_card_iphone_basic,
                FakeCallScreenThemeStore.IncomingCallUiBrand.IPHONE,
                R.drawable.mini_preview_iphone,
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
        val startTab = tabIndexForBrand(initial)
        adapter =
            CallThemesAdapter(
                items = themesForTab(startTab),
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

        binding.themeTabs.getTabAt(startTab)?.select()

        binding.themeTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val position = tab?.position ?: return
                    val items = themesForTab(position)
                    if (items.none { it.brand == adapter.selectedBrand() }) {
                        adapter.setSelectedBrand(items.first().brand)
                    }
                    adapter.submitList(items)
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

    private fun themesForTab(position: Int): List<CallThemesAdapter.ThemeCardItem> =
        when (position) {
            1 -> onePlusThemes
            2 -> iPhoneThemes
            else -> samsungThemes
        }

    private fun tabIndexForBrand(brand: FakeCallScreenThemeStore.IncomingCallUiBrand): Int =
        when (brand) {
            FakeCallScreenThemeStore.IncomingCallUiBrand.ONEPLUS -> 1
            FakeCallScreenThemeStore.IncomingCallUiBrand.IPHONE -> 2
            FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_ONE_UI,
            FakeCallScreenThemeStore.IncomingCallUiBrand.SAMSUNG_SWIPE_UP,
            -> 0
        }
}
