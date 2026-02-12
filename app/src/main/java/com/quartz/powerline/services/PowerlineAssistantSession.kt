package com.quartz.powerline.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import com.quartz.powerline.OverlayActivity

class PowerlineAssistantSession(context: Context) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "PowerlineAssistant"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PowerlineAssistantSession created")
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "onShow called with flags: $showFlags")

        // Launch OverlayActivity when the assistant is triggered
        val intent = Intent(context, OverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // It's often good practice for overlays to disable animation to feel snappier
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }

        try {
            startAssistantActivity(intent)
            Log.d(TAG, "startAssistantActivity called")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start assistant activity", e)
            // Fallback
            try {
                context.startActivity(intent)
                Log.d(TAG, "Fallback context.startActivity called")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed fallback start activity", e2)
            }
        }

        // Hide the assistant session window immediately since we're launching an activity
        hide()
    }

    override fun onHandleAssist(
        data: Bundle?,
        structure: android.app.assist.AssistStructure?,
        content: android.app.assist.AssistContent?
    ) {
        super.onHandleAssist(data, structure, content)
        Log.d(TAG, "onHandleAssist called")
    }
}
