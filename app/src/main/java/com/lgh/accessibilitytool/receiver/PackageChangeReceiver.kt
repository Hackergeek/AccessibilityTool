package com.lgh.accessibilitytool.receiver

import android.content.*
import com.lgh.accessibilitytool.service.GestureAccessibilityService
import com.lgh.accessibilitytool.service.NoGestureAccessibilityService

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (GestureAccessibilityService.mainFunctions != null) {
            GestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(0x02)
        }
        if (NoGestureAccessibilityService.mainFunctions != null) {
            NoGestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(0x02)
        }
    }
}