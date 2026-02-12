package com.quartz.powerline

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.utils.GoogleMobileAdsConsentManager
import kotlinx.coroutines.launch

class WelcomeActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(this)
        val consentManager = GoogleMobileAdsConsentManager.getInstance(this)

        setContent {
            CommandLauncherTheme {
                WelcomeScreen(
                    settingsRepository = settingsRepository,
                    consentManager = consentManager,
                    onFinish = {
                        settingsRepository.setOnboardingCompleted(true)
                        val intent = Intent(this, OverlayActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    settingsRepository: SettingsRepository,
    consentManager: GoogleMobileAdsConsentManager,
    onFinish: () -> Unit
) {
    var initialPage by rememberSaveable { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 6 })

    // Update the saved initialPage whenever the page changes, so if the activity restarts,
    // we start from the last viewed page.
    LaunchedEffect(pagerState.currentPage) {
        initialPage = pagerState.currentPage
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = LocalContext.current as? androidx.activity.ComponentActivity

    // Observe permission states
    var isAccessibilityGranted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        // Accessibility check is a bit manual
        val accessibilityEnabled = try {
             Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1
        } catch (e: Settings.SettingNotFoundException) {
            false
        }

        if (accessibilityEnabled) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            isAccessibilityGranted = settingValue?.contains("${context.packageName}/") == true
        } else {
            isAccessibilityGranted = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Manage Debug Mode for Page 5 (Handle Adjustment)
    // We only enable debug mode when we are ON page 4 (index 4) (0-indexed).
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 4) {
            settingsRepository.setDebugMode(true)
        } else {
            // Ensure we turn it off if they swipe back or forward (though forward ends activity)
            settingsRepository.setDebugMode(false)
        }
    }

    // Clean up on exit
    DisposableEffect(Unit) {
        onDispose {
            settingsRepository.setDebugMode(false)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = false // We control navigation via buttons to ensure flow
            ) { page ->
                when (page) {
                    0 -> WelcomePage(
                        onNext = { scope.launch { pagerState.animateScrollToPage(1) } }
                    )
                    1 -> AppRolePage(
                        onNext = { scope.launch { pagerState.animateScrollToPage(2) } }
                    )
                    2 -> SetHomePage(
                        onSkip = { scope.launch { pagerState.animateScrollToPage(3) } },
                        onNext = { scope.launch { pagerState.animateScrollToPage(3) } }
                    )
                    3 -> SetOverlayPage(
                        isAccessibilityGranted = isAccessibilityGranted,
                        onSkip = {
                            // Disable features if skipped
                            settingsRepository.setBottomTriggerEnabled(false)
                            scope.launch { pagerState.animateScrollToPage(5) }
                        },
                        onNext = {
                            // Enable features
                            settingsRepository.setBottomTriggerEnabled(true)
                            // Default config if needed
                            scope.launch { pagerState.animateScrollToPage(4) }
                        }
                    )
                    4 -> SetHandlePage(
                        settingsRepository = settingsRepository,
                        onNext = { scope.launch { pagerState.animateScrollToPage(5) } }
                    )
                    5 -> SupportPage(
                        consentManager = consentManager,
                        activity = activity,
                        onFinish = onFinish
                    )
                }
            }

            // Pager Indicator (Optional, but nice)
            Row(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(6) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(8.dp)
                            .background(color, shape = MaterialTheme.shapes.small)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to Powerline Launcher",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Efficiency at your fingertips.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}

@Composable
fun AppRolePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "What can you do?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Powerline Launcher allows you to access apps, settings, and tools instantly from anywhere.\n\n" +
                   "Just type what you need, or use the overlay trigger to launch your apps without leaving your current screen.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Got it")
        }
    }
}

@Composable
fun SetHomePage(onSkip: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Home, null, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Set as Default Home",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To get the best experience, set Powerline Launcher as your default home app.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Home Settings")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
             TextButton(onClick = onSkip) {
                Text("Skip")
            }
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
fun SetOverlayPage(
    isAccessibilityGranted: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Layers, null, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Enable Overlay",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Access the launcher from any app using corner triggers.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Accessibility Permission
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isAccessibilityGranted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
        ) {
             Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Accessibility Service")
                Spacer(modifier = Modifier.weight(1f))
                if (isAccessibilityGranted) Icon(Icons.Default.Check, null)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
             TextButton(onClick = onSkip) {
                Text("Don't use Overlays")
            }
            Button(
                onClick = onNext,
                enabled = isAccessibilityGranted
            ) {
                Text("Enable & Continue")
            }
        }
    }
}

@Composable
fun SetHandlePage(
    settingsRepository: SettingsRepository,
    onNext: () -> Unit
) {
    var gapPercent by remember { mutableStateOf(settingsRepository.getBottomTriggerCenterGapPercent()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Settings, null, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Adjust Triggers",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Use the slider to adjust the gap between the bottom triggers so they don't overlap with your navigation bar.\n\n(You should see red zones on the screen now)",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text("Center Gap: $gapPercent%")
        Slider(
            value = gapPercent.toFloat(),
            onValueChange = {
                gapPercent = it.toInt()
                settingsRepository.setBottomTriggerCenterGapPercent(gapPercent)
            },
            valueRange = 0f..90f,
            steps = 17
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Next")
        }
    }
}

@Composable
fun SupportPage(
    consentManager: GoogleMobileAdsConsentManager,
    activity: androidx.activity.ComponentActivity?,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Settings, null, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Support Powerline",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To keep Powerline free, we show ads in the Settings menu. You can remove them by upgrading to Advanced.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                context.startActivity(Intent(context, PurchaseActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upgrade to Advanced")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                if (activity != null) {
                    isLoading = true
                    consentManager.gatherConsent(activity) { error ->
                        isLoading = false
                        if (error != null) {
                            // Log error?
                            android.util.Log.e("WelcomeActivity", "Consent error: ${error.message}")
                        }
                        onFinish()
                    }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Continue with Ads")
            }
        }
    }
}
