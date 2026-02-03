package com.quartz.powerline.services

import android.content.Intent
import android.service.quicksettings.TileService
import com.quartz.powerline.OverlayActivity

class QuickSettingsService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, OverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Add flags to ensure we don't just bring an existing instance forward if we want a fresh state?
            // SingleInstance covers it.
        }

        // This method handles the background start restriction automatically
        startActivityAndCollapse(intent)
    }
}
