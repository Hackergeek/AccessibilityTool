package com.lgh.accessibilitytool

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

class ScreenLightness constructor(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var params: WindowManager.LayoutParams? = null
    private var win: Window? = null
    private var imageView: ImageView? = null
    private val sharedPreferences =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    private var argb: Int
    fun showFloat() {
        if (imageView != null) return
        params = WindowManager.LayoutParams()
        params!!.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        params!!.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params!!.format = PixelFormat.TRANSPARENT
        params!!.gravity = Gravity.START or Gravity.TOP
        val metrics = DisplayMetrics()
        context.display?.getRealMetrics(metrics)
        params!!.width = metrics.widthPixels
        params!!.height = metrics.heightPixels
        params!!.alpha = 1f
        imageView = ImageView(context)
        imageView!!.setBackgroundColor(argb)
        windowManager.addView(imageView, params)
    }

    fun dismiss() {
        if (imageView != null) {
            windowManager.removeViewImmediate(imageView)
            params = null
            imageView = null
        }
    }

    fun refreshOnOrientationChange() {
        if (imageView != null) {
            val metrics = DisplayMetrics()
            context.display?.getRealMetrics(metrics)
            if (params!!.width != metrics.widthPixels || params!!.height != metrics.heightPixels) {
                params!!.width = metrics.widthPixels
                params!!.height = metrics.heightPixels
                windowManager.updateViewLayout(imageView, params)
            }
        }
    }

    fun showControlDialog() {
        val view: View = LayoutInflater.from(context).inflate(R.layout.screen_lightness_set, null)
        val seekBarR: SeekBar = view.findViewById(R.id.seekBarR)
        val seekBarG: SeekBar = view.findViewById(R.id.seekBarG)
        val seekBarB: SeekBar = view.findViewById(R.id.seekBarB)
        val seekBarA: SeekBar = view.findViewById(R.id.seekBarA)
        seekBarR.progress = Color.red(argb)
        seekBarG.progress = Color.green(argb)
        seekBarB.progress = Color.blue(argb)
        seekBarA.progress = Color.alpha(argb)
        val handleSeekBar = HandleSeekBar()
        seekBarR.setOnSeekBarChangeListener(handleSeekBar)
        seekBarG.setOnSeekBarChangeListener(handleSeekBar)
        seekBarB.setOnSeekBarChangeListener(handleSeekBar)
        seekBarA.setOnSeekBarChangeListener(handleSeekBar)
        val dialog: AlertDialog = AlertDialog.Builder(context).setView(view)
            .setOnDismissListener { sharedPreferences.edit().putInt(ARGB, argb).apply() }.create()
        win = dialog.window
        win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
        win!!.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        win!!.setDimAmount(0f)
        dialog.show()
        val windowLayoutParams: WindowManager.LayoutParams = win!!.attributes
        val metrics = DisplayMetrics()
        context.display?.getRealMetrics(metrics)
        if (metrics.heightPixels > metrics.widthPixels) {
            windowLayoutParams.width = (metrics.widthPixels / 6) * 5
            windowLayoutParams.height = metrics.heightPixels / 2
        } else {
            windowLayoutParams.width = (metrics.heightPixels / 6) * 5
            windowLayoutParams.height = metrics.widthPixels / 2
        }
        win!!.attributes = windowLayoutParams
    }

    internal inner class HandleSeekBar : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            when (seekBar.id) {
                R.id.seekBarR -> argb = (argb and -0xff0001) or progress shl 16
                R.id.seekBarG -> argb = (argb and -0xff01) or progress shl 8
                R.id.seekBarB -> argb = (argb and -0x100) or progress
                R.id.seekBarA -> argb = (argb and 0x00ffffff) or progress shl 24
            }
            if (imageView != null) {
                imageView!!.setBackgroundColor(argb)
                windowManager.updateViewLayout(imageView, params)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            win!!.setBackgroundDrawableResource(R.drawable.transparent_dialog)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
        }
    }

    companion object {
        private const val ARGB: String = "argb"
    }

    init {
        argb = sharedPreferences.getInt(ARGB, 0x00000000)
    }
}