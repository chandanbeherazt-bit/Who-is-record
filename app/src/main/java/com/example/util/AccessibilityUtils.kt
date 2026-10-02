package com.example.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import com.example.service.VolumeHoldAccessibilityService

object AccessibilityUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${VolumeHoldAccessibilityService::class.java.canonicalName}"
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            val serviceInfoId = service.id
            if (serviceInfoId.contains(context.packageName) &&
                serviceInfoId.contains("VolumeHoldAccessibilityService")) {
                return true
            }
        }

        // Fallback check via Settings.Secure
        try {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabled)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    (componentName.contains(context.packageName) && componentName.contains("VolumeHoldAccessibilityService"))) {
                    return true
                }
            }
        } catch (_: Exception) {
        }
        return false
    }
}
