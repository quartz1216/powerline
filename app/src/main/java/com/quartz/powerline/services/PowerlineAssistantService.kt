package com.quartz.powerline.services

import android.content.Intent
import android.os.IBinder
import android.service.voice.VoiceInteractionService
import android.util.Log

class PowerlineAssistantService : VoiceInteractionService() {
    companion object {
        private const val TAG = "PowerlineAssistantSvc"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PowerlineAssistantService created")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "PowerlineAssistantService bound with intent: $intent")
        return super.onBind(intent)
    }
}
