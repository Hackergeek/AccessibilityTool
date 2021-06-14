package com.lgh.accessibilitytool

import android.content.*

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (MyAccessibilityService.mainFunctions != null) {
            MyAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(0x02)
        }
        if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
            MyAccessibilityServiceNoGesture.mainFunctions!!.handler!!.sendEmptyMessage(0x02)
        }
    }
}