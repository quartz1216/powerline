package com.quartz.powerline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.quartz.powerline.ui.AdditionalSettingsSubpage
import com.quartz.powerline.ui.AppearanceSettingsScreen
import com.quartz.powerline.ui.OverlaySettingsSubpage
import com.quartz.powerline.ui.PermissionsScreen
import com.quartz.powerline.ui.PluginSettingsScreen
import com.quartz.powerline.ui.SettingsScreen
import com.quartz.powerline.ui.components.AdMobBanner
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.SettingsNav
import com.quartz.powerline.ui.viewmodel.SettingsViewModel
import com.quartz.powerline.utils.GoogleMobileAdsConsentManager

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val consentManager = GoogleMobileAdsConsentManager.getInstance(this)
        consentManager.gatherConsent(this) { error ->
            if (error != null) {
                android.util.Log.e("SettingsActivity", "Consent gathering failed: ${error.message}")
            }
        }

        val viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            val materialYouEnabled by viewModel.materialYouEnabled.collectAsState()
            CommandLauncherTheme(dynamicColor = materialYouEnabled) {
                val currentNav by viewModel.currentNav.collectAsState()

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Handle system back button to navigate within settings or finish activity
                        BackHandler {
                            if (!viewModel.navigateBack()) {
                                finish()
                            }
                        }

                        when (currentNav) {
                            SettingsNav.MAIN -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { finish() }
                                )
                            }
                            SettingsNav.OVERLAY -> {
                                OverlaySettingsSubpage(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            SettingsNav.ADDITIONAL -> {
                                AdditionalSettingsSubpage(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            SettingsNav.APPEARANCE -> {
                                AppearanceSettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            SettingsNav.PERMISSIONS -> {
                                PermissionsScreen(
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                            SettingsNav.PLUGIN -> {
                                val pluginId by viewModel.selectedPluginId.collectAsState()
                                val plugin = pluginId?.let { viewModel.pluginManager.getPlugin(it) }
                                if (plugin != null) {
                                    PluginSettingsScreen(
                                        viewModel = viewModel,
                                        plugin = plugin,
                                        onBack = { viewModel.navigateBack() }
                                    )
                                } else {
                                    viewModel.navigateBack()
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        AdMobBanner()
                    }
                }
            }
        }
    }
}
