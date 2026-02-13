package com.quartz.powerline.services

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log

class PowerlineAssistantSessionService : VoiceInteractionSessionService() {

    companion object {
        private const val TAG = "PowerlineAssistantSessSvc"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PowerlineAssistantSessionService created")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "PowerlineAssistantSessionService bound with intent: $intent")
        return super.onBind(intent)
    }

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.d(TAG, "Creating new PowerlineAssistantSession")
        return PowerlineAssistantSession(this)
    }
}
