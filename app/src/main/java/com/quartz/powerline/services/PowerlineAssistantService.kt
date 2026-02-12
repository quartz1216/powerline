package com.quartz.powerline.services

import android.service.voice.VoiceInteractionService

class PowerlineAssistantService : VoiceInteractionService() {
    // This class is the entry point for the system to recognize the app as a Voice Interaction Service.
    // It doesn't need to implement anything specific if we just want to launch an activity on trigger.
    // The actual logic is handled in the Session (PowerlineAssistantSession).
}
