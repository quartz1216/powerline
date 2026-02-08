package com.quartz.powerline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.MobileAds
import com.quartz.powerline.ui.AppearanceSettingsScreen
import com.quartz.powerline.ui.PermissionsScreen
import com.quartz.powerline.ui.PluginSettingsScreen
import com.quartz.powerline.ui.SettingsScreen
import com.quartz.powerline.ui.OverlaySettingsSubpage
import com.quartz.powerline.ui.AdditionalSettingsSubpage
import com.quartz.powerline.ui.components.AdBanner
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.SettingsNav
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize AdMob
        try {
            MobileAds.initialize(this) { status ->
                android.util.Log.d("SettingsActivity", "MobileAds initialized: $status")
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "MobileAds init failed", e)
        }

        val viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            val materialYouEnabled by viewModel.materialYouEnabled.collectAsState()
            val isAdvanced by viewModel.isAdvanced.collectAsState()

            CommandLauncherTheme(dynamicColor = if (isAdvanced) materialYouEnabled else false) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        val currentNav by viewModel.currentNav.collectAsState()

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

                    if (!isAdvanced) {
                        AdBanner()
                    }
                }
            }
        }
    }
}
