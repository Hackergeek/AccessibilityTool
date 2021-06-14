package com.lgh.accessibilitytool

import android.content.*

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action: String? = intent.action
            if (action != null) {
                if ((action == Intent.ACTION_SCREEN_ON)) {
                    if (MyAccessibilityService.mainFunctions != null) {
                        MyAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(
                            0x05
                        )
                    }
                    if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                        MyAccessibilityServiceNoGesture.mainFunctions!!.handler!!.sendEmptyMessage(
                            0x05
                        )
                    }
                }
                if ((action == Intent.ACTION_SCREEN_OFF)) {
                    if (MyAccessibilityService.mainFunctions != null) {
                        MyAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(
                            0x03
                        )
                    }
                    if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                        MyAccessibilityServiceNoGesture.mainFunctions!!.handler!!.sendEmptyMessage(
                            0x03
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}