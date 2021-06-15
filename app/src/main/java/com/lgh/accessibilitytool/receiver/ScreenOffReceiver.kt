package com.lgh.accessibilitytool.receiver

import android.content.*
import com.lgh.accessibilitytool.service.GestureAccessibilityService
import com.lgh.accessibilitytool.service.NoGestureAccessibilityService

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action: String? = intent.action
            if (action != null) {
                if ((action == Intent.ACTION_SCREEN_ON)) {
                    if (GestureAccessibilityService.mainFunctions != null) {
                        GestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(
                            0x05
                        )
                    }
                    if (NoGestureAccessibilityService.mainFunctions != null) {
                        NoGestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(
                            0x05
                        )
                    }
                }
                if ((action == Intent.ACTION_SCREEN_OFF)) {
                    if (GestureAccessibilityService.mainFunctions != null) {
                        GestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(
                            0x03
                        )
                    }
                    if (NoGestureAccessibilityService.mainFunctions != null) {
                        NoGestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(
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