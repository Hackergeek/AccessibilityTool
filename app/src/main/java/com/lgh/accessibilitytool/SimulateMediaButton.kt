package com.lgh.accessibilitytool

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

class SimulateMediaButton constructor(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    fun sendMediaButton(keycode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keycode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keycode)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

}