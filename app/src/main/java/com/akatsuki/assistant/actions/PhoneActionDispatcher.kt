package com.akatsuki.assistant.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import com.akatsuki.assistant.brain.AkatsukiCommand

class PhoneActionDispatcher(private val context: Context) {

    private val tag = "PhoneActionDispatcher"

    fun execute(command: AkatsukiCommand, onSpokenFeedback: (String) -> Unit) {
        try {
            when (command.action.uppercase()) {
                "CALL" -> {
                    makePhoneCall(command.target, onSpokenFeedback)
                }
                "SMS" -> {
                    sendSms(command.target, command.data, onSpokenFeedback)
                }
                "WHATSAPP" -> {
                    openWhatsApp(command.target, command.data, onSpokenFeedback)
                }
                "TORCH" -> {
                    val turnOn = command.target.contains("ON", ignoreCase = true)
                    toggleTorch(turnOn, onSpokenFeedback)
                }
                "OPEN_APP" -> {
                    openApplication(command.target, onSpokenFeedback)
                }
                "ALARM" -> {
                    setAlarm(command.target, command.data, onSpokenFeedback)
                }
                "BATTERY" -> {
                    checkBattery(onSpokenFeedback)
                }
                else -> {
                    // Normal Chat or general query
                    onSpokenFeedback(command.speech)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute action ${command.action}: ${e.message}", e)
            onSpokenFeedback("Action perform karne me dikkat aayi, Sir.")
        }
    }

    private fun makePhoneCall(target: String, onFeedback: (String) -> Unit) {
        val phoneNumber = if (target.matches(Regex("^[0-9+\\- ]+$"))) {
            target
        } else {
            resolveContactNumber(target)
        }

        if (!phoneNumber.isNullOrBlank()) {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(callIntent)
                onFeedback("$target ko call mila raha hoon, Sir.")
            } catch (e: SecurityException) {
                // If CALL_PHONE permission not granted, fallback to DIAL
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                onFeedback("$target ka number dial pad par open kar diya hai.")
            }
        } else {
            onFeedback("Contacts me $target ka number nahi mila, Sir.")
        }
    }

    private fun resolveContactNumber(name: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    return it.getString(numberIndex)
                }
            }
        }
        return null
    }

    private fun sendSms(target: String, message: String, onFeedback: (String) -> Unit) {
        val number = resolveContactNumber(target) ?: target
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(number, null, message, null, null)
            onFeedback("$target ko SMS bhej diya gaya hai, Sir.")
        } catch (e: Exception) {
            // Fallback to SMS App intent
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            onFeedback("$target ke liye SMS app open kar diya hai.")
        }
    }

    private fun openWhatsApp(target: String, message: String, onFeedback: (String) -> Unit) {
        val pm = context.packageManager
        val whatsappIntent = pm.getLaunchIntentForPackage("com.whatsapp")
        if (whatsappIntent != null) {
            whatsappIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(whatsappIntent)
            onFeedback("WhatsApp open kar diya hai, Sir.")
        } else {
            onFeedback("Phone me WhatsApp installed nahi mila, Sir.")
        }
    }

    private fun toggleTorch(turnOn: Boolean, onFeedback: (String) -> Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, turnOn)
            if (turnOn) {
                onFeedback("Torch on kar di hai, Sir.")
            } else {
                onFeedback("Torch band kar di hai, Sir.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Torch control error: ${e.message}")
            onFeedback("Torch access karne me problem aayi.")
        }
    }

    private fun openApplication(appName: String, onFeedback: (String) -> Unit) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        var targetPackage: String? = null
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(appName, ignoreCase = true) || app.packageName.contains(appName, ignoreCase = true)) {
                targetPackage = app.packageName
                break
            }
        }

        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                onFeedback("$appName open kar diya hai, Sir.")
                return
            }
        }

        onFeedback("$appName phone me nahi mila, Sir.")
    }

    private fun setAlarm(timeStr: String, label: String, onFeedback: (String) -> Unit) {
        try {
            // Expected format like "07:30" or "7:30"
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].trim().toInt()
                val minute = parts[1].trim().toInt()

                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, if (label.isNotBlank()) label else "Akatsuki Alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                onFeedback("$timeStr ka alarm set kar diya hai, Sir.")
                return
            }
        } catch (e: Exception) {
            Log.e(tag, "Alarm setting failed: ${e.message}")
        }
        onFeedback("Alarm set karne me problem aayi, Sir.")
    }

    private fun checkBattery(onFeedback: (String) -> Unit) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = bm.isCharging
        val statusMsg = if (isCharging) {
            "Aapka phone abhi $batLevel percent charged hai aur charging par laga hai, Sir."
        } else {
            "Aapke phone me abhi $batLevel percent battery bachi hai, Sir."
        }
        onFeedback(statusMsg)
    }
}
