package com.lgh.accessibilitytool

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {
    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val context = applicationContext
            if (MyAccessibilityService.mainFunctions == null && MyAccessibilityServiceNoGesture.Companion.mainFunctions == null) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Toast.makeText(context, "请打开其中一个无障碍服务", Toast.LENGTH_SHORT).show()
            } else if (MyAccessibilityService.mainFunctions != null && MyAccessibilityServiceNoGesture.Companion.mainFunctions != null) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Toast.makeText(context, "无障碍服务冲突，请关闭其中一个", Toast.LENGTH_SHORT).show()
            } else {
                if (MyAccessibilityService.mainFunctions != null) {
                    MyAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(0x00)
                }
                if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                    MyAccessibilityServiceNoGesture.mainFunctions!!.handler!!.sendEmptyMessage(
                        0x00
                    )
                }
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                        "package:$packageName"
                    )
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                Toast.makeText(context, "请授予读写手机存储权限，并设置允许后台运行", Toast.LENGTH_SHORT).show()
            }
            if (!Settings.canDrawOverlays(context)) {
                val drawOverlayIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(
                        "package:$packageName"
                    )
                )
                drawOverlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val resolveInfo =
                    packageManager.resolveActivity(drawOverlayIntent, PackageManager.MATCH_ALL)
                if (resolveInfo != null) {
                    startActivity(drawOverlayIntent)
                } else {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                            "package:$packageName"
                        )
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                Toast.makeText(context, "请授予应用悬浮窗权限，并设置允许后台运行", Toast.LENGTH_SHORT).show()
            }
            if (!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                    packageName
                )
            ) {
                val powerOptimizations = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse(
                        "package:$packageName"
                    )
                )
                powerOptimizations.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val resolveInfo =
                    packageManager.resolveActivity(powerOptimizations, PackageManager.MATCH_ALL)
                if (resolveInfo != null) startActivity(powerOptimizations)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        finish()
    }

    companion object {
        var TAG = "MainActivity"
    }
}