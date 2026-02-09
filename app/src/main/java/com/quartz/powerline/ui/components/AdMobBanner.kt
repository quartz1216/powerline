package com.quartz.powerline.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidth))
                adUnitId = "ca-app-pub-3940256099942544/9214589741"
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdMobBanner", "Ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("AdMobBanner", "Ad failed to load: ${error.code} - ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = {
            it.destroy()
        }
    )
}
