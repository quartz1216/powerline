package com.quartz.powerline.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.quartz.powerline.data.HistoryItem
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult

object ActionExecutor {

    fun execute(context: Context, result: SearchResult, settingsRepository: SettingsRepository) {
        var success = false

        when (result.type) {
            ResultType.SYSTEM -> {
                when (result.payload) {
                    "settings_action" -> {
                        // Launch MainActivity which will handle opening the settings screen
                        val intent = Intent(context, com.quartz.powerline.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("open_settings", true)
                        }
                        context.startActivity(intent)
                        success = true
                    }
                    "wifi" -> success = openSettings(context, Settings.ACTION_WIFI_SETTINGS)
                    "bluetooth" -> success = openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)
                    "flashlight" -> success = toggleFlashlight(context)
                    "volume" -> {
                        success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                context.startActivity(Intent(Settings.Panel.ACTION_VOLUME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                true
                            } catch (e: Exception) {
                                openSettings(context, Settings.ACTION_SOUND_SETTINGS)
                            }
                        } else {
                            openSettings(context, Settings.ACTION_SOUND_SETTINGS)
                        }
                    }
                    "battery" -> success = openSettings(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
                    "airplane" -> success = openSettings(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                    "brightness" -> success = openSettings(context, Settings.ACTION_DISPLAY_SETTINGS)
                    "dnd" -> success = openSettings(context, Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
                }
            }
            ResultType.APP -> {
                if (result.packageName != null) {
                    val intent = context.packageManager.getLaunchIntentForPackage(result.packageName)
                    if (intent != null) {
                        context.startActivity(intent)
                        success = true
                    } else {
                        Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ResultType.WEB -> {
                val url = if (result.payload!!.startsWith("http")) result.payload else "https://www.google.com/search?q=${Uri.encode(result.payload)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    success = true
                } catch (e: Exception) {
                    Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                }
            }
            ResultType.CALC -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Calculation Result", result.payload)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied: ${result.payload}", Toast.LENGTH_SHORT).show()
                success = true
            }
        }

        if (success) {
            val historyItem = HistoryItem(
                id = result.id,
                type = result.type,
                title = result.title,
                timestamp = System.currentTimeMillis()
            )
            settingsRepository.addToHistory(historyItem)
        }
    }

    private fun openSettings(context: Context, action: String): Boolean {
        return try {
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private var isFlashlightOn = false

    private fun toggleFlashlight(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Camera permission required for flashlight", Toast.LENGTH_SHORT).show()
            // We should ideally request permission here, but ActionExecutor is an object and doesn't have an activity context.
            // MainActivity handles the initial permission check, but if denied, we can't easily request here.
            // We'll rely on MainActivity to handle the request if needed.
            val intent = Intent(context, com.quartz.powerline.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("request_camera_permission", true)
            }
            context.startActivity(intent)
            return false
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val cameraId = cameraManager.cameraIdList.getOrNull(0)
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId, isFlashlightOn)
                true
            } else {
                Toast.makeText(context, "No flashlight available", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
