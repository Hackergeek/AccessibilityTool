package com.lgh.accessibilitytool.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.view.*
import android.view.accessibility.AccessibilityEvent
import com.lgh.accessibilitytool.MainFunctions

class GestureAccessibilityService : AccessibilityService() {
    private var createNum: Int = 0
    private var connectNum: Int = 0
    override fun onCreate() {
        super.onCreate()
        try {
            createNum = 0
            connectNum = 0
            createNum++
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (++connectNum != createNum) {
            throw RuntimeException("无障碍服务出现异常")
        }
        mainFunctions = MainFunctions(this)
        mainFunctions!!.onServiceConnected()
        //停止MyAccessibilityServiceNoGesture辅助性服务
        if (NoGestureAccessibilityService.mainFunctions != null) {
            NoGestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(MainFunctions.MESSAGE_DISABLE_SELF)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        mainFunctions!!.onAccessibilityEvent(event)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return mainFunctions!!.onKeyEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mainFunctions!!.onConfigurationChanged(newConfig)
    }

    override fun onUnbind(intent: Intent): Boolean {
        mainFunctions!!.onUnbind(intent)
        mainFunctions = null
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    companion object {
        var mainFunctions: MainFunctions? = null
    }
}