package com.quartz.powerline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.ui.theme.CommandLauncherTheme

class PurchaseActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(this)

        setContent {
            CommandLauncherTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Powerline Advanced") },
                            actions = {
                                IconButton(onClick = {
                                    val fromOnboarding = intent.getBooleanExtra("from_onboarding", false)
                                    if (fromOnboarding) {
                                        settingsRepository.setOnboardingCompleted(true)
                                        val mainIntent = Intent(this@PurchaseActivity, OverlayActivity::class.java)
                                        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(mainIntent)
                                    }
                                    finish()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Unlock Full Potential",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Support the development and unlock future premium features.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        // Placeholder for Billing UI
                        Button(
                            onClick = { /* TODO: Implement billing */ },
                            enabled = false // Disabled for now as per instructions (just a placeholder/page)
                        ) {
                            Text("Purchase (Coming Soon)")
                        }
                    }
                }
            }
        }
    }
}
