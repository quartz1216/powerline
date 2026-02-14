package com.quartz.powerline.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class GoogleMobileAdsConsentManager private constructor(context: Context) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val isMobileAdsSdkInitialized = AtomicBoolean(false)

    private val _canRequestAdsState = MutableStateFlow(consentInformation.canRequestAds())
    val canRequestAdsState: StateFlow<Boolean> = _canRequestAdsState.asStateFlow()

    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        // Create a ConsentRequestParameters object.
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // Consent gathering process complete.
                    _canRequestAdsState.value = canRequestAds
                    if (canRequestAds) {
                        initializeMobileAdsSdk(activity)
                    }
                    onConsentGatheringCompleteListener.consentGatheringComplete(formError)
                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                _canRequestAdsState.value = canRequestAds
                onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
            }
        )
    }

    fun initializeMobileAdsSdk(context: Context) {
        if (isMobileAdsSdkInitialized.getAndSet(true)) {
            return
        }

        _canRequestAdsState.value = canRequestAds
        if (canRequestAds) {
            MobileAds.initialize(context)
        } else {
            isMobileAdsSdkInitialized.set(false)
        }
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    companion object {
        @Volatile private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context): GoogleMobileAdsConsentManager {
            return instance ?: synchronized(this) {
                instance ?: GoogleMobileAdsConsentManager(context).also { instance = it }
            }
        }
    }
}
