package com.quartz.powerline.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.quartz.powerline.utils.GoogleMobileAdsConsentManager

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val consentManager = GoogleMobileAdsConsentManager.getInstance(context)

    if (consentManager.canRequestAds) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidth)

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = "ca-app-pub-3940256099942544/9214589741"
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("AdMobBanner", "Ad loaded successfully")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e("AdMobBanner", "Ad failed to load: ${error.code} - ${error.message}")
                        }

                        override fun onAdOpened() {
                            Log.d("AdMobBanner", "Ad opened")
                        }

                        override fun onAdClicked() {
                            Log.d("AdMobBanner", "Ad clicked")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = {
                it.destroy()
            }
        )
    } else {
        Log.d("AdMobBanner", "Cannot request ads: consent not granted or not yet gathered")
    }
}
