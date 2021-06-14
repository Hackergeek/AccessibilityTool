package com.lgh.accessibilitytool

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.LinearLayout

class HelpActivity : Activity() {
    private lateinit var sharedPreferences: SharedPreferences

    @get:JavascriptInterface
    @set:JavascriptInterface
    var autoRemove = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        sharedPreferences = getSharedPreferences(packageName, MODE_PRIVATE)
        autoRemove = sharedPreferences.getBoolean(AUTO_REMOVE, true)
        val webView = findViewById<WebView>(R.id.webView)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        webView.addJavascriptInterface(this, "HelpActivity")
        val resources = resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val params = LinearLayout.LayoutParams(webView.layoutParams)
        params.topMargin = resources.getDimensionPixelSize(resourceId) + 5
        webView.layoutParams = params
        webView.loadUrl("file:///android_asset/help.html")
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            sharedPreferences.edit().putBoolean(AUTO_REMOVE, autoRemove).apply()
            if (autoRemove) {
                finishAndRemoveTask()
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onStop() {
        super.onStop()
        sharedPreferences.edit().putBoolean(AUTO_REMOVE, autoRemove).apply()
        if (autoRemove) {
            finishAndRemoveTask()
        }
    }

    @JavascriptInterface
    fun chooseService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    companion object {
        const val AUTO_REMOVE = "autoRemove"
    }
}