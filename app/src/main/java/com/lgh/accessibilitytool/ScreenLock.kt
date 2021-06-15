package com.lgh.accessibilitytool

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.lgh.accessibilitytool.service.GestureAccessibilityService
import com.lgh.accessibilitytool.service.NoGestureAccessibilityService

class ScreenLock constructor(private val context: Context) {
    private var width: Int
    private var height: Int
    private var px: Int
    private var py: Int
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val sharedPreferences =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val metrics = DisplayMetrics()
    private var imageView: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var view: View? = null
    fun showLockFloat() {
        if (imageView != null) return
        params = WindowManager.LayoutParams()
        params!!.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        params!!.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params!!.format = PixelFormat.TRANSPARENT
        params!!.gravity = Gravity.START or Gravity.TOP
        params!!.alpha = 0.5f
        params!!.width = width
        params!!.height = height
        params!!.x = px
        params!!.y = py
        imageView = ImageView(context)
        imageView!!.setImageResource(R.drawable.launch)
        imageView!!.setBackgroundColor(Color.BLACK)
        val clickListener: View.OnClickListener = object : View.OnClickListener {
            var start: Long = System.currentTimeMillis()
            override fun onClick(v: View) {
                val end: Long = System.currentTimeMillis()
                if ((end - start) < 800) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (GestureAccessibilityService.mainFunctions != null) {
                            GestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(
                                MainFunctions.MESSAGE_LOCK_SCREEN
                            )
                        }
                        if (NoGestureAccessibilityService.mainFunctions != null) {
                            NoGestureAccessibilityService.mainFunctions!!.handler!!.sendEmptyMessage(
                                MainFunctions.MESSAGE_LOCK_SCREEN
                            )
                        }
                    } else {
                        devicePolicyManager.lockNow()
                    }
                }
                start = end
            }
        }
        imageView!!.setOnClickListener(clickListener)
        windowManager.addView(imageView, params)
    }

    fun dismiss() {
        if (imageView != null) {
            windowManager.removeViewImmediate(imageView)
            imageView = null
            params = null
        }
        if (view != null) {
            view!!.findViewById<View>(R.id.seekBarWidth).isEnabled = false
            view!!.findViewById<View>(R.id.seekBarHeight).isEnabled = false
            view!!.findViewById<View>(R.id.seekBarX).isEnabled = false
            view!!.findViewById<View>(R.id.seekBarY).isEnabled = false
        }
    }

    fun showSetAreaDialog() {
        view = LayoutInflater.from(context).inflate(R.layout.screen_lock_position, null)
        val seekBarWidth: SeekBar = view!!.findViewById(R.id.seekBarWidth)
        val seekBarHeight: SeekBar = view!!.findViewById(R.id.seekBarHeight)
        val seekBarX: SeekBar = view!!.findViewById(R.id.seekBarX)
        val seekBarY: SeekBar = view!!.findViewById(R.id.seekBarY)
        seekBarWidth.max = metrics.widthPixels / 4
        seekBarHeight.max = metrics.heightPixels / 4
        seekBarX.max = metrics.widthPixels
        seekBarY.max = metrics.heightPixels
        seekBarWidth.progress = width
        seekBarHeight.progress = height
        seekBarX.progress = px
        seekBarY.progress = py
        if (imageView == null) {
            seekBarWidth.isEnabled = false
            seekBarHeight.isEnabled = false
            seekBarX.isEnabled = false
            seekBarY.isEnabled = false
        }
        val onSeekBarChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (imageView == null || params == null) return
                when (seekBar.id) {
                    R.id.seekBarWidth -> params!!.width = i
                    R.id.seekBarHeight -> params!!.height = i
                    R.id.seekBarX -> params!!.x = i
                    R.id.seekBarY -> params!!.y = i
                }
                windowManager.updateViewLayout(imageView, params)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        seekBarWidth.setOnSeekBarChangeListener(onSeekBarChangeListener)
        seekBarHeight.setOnSeekBarChangeListener(onSeekBarChangeListener)
        seekBarX.setOnSeekBarChangeListener(onSeekBarChangeListener)
        seekBarY.setOnSeekBarChangeListener(onSeekBarChangeListener)
        val dialog: AlertDialog = AlertDialog.Builder(context).setView(view)
            .setOnDismissListener {
                if (imageView != null && params != null) {
                    width = params!!.width
                    height = params!!.height
                    px = params!!.x
                    py = params!!.y
                    sharedPreferences.edit().putInt(WIDTH, width).putInt(HEIGHT, height).putInt(
                        POSITION_X, px
                    ).putInt(POSITION_Y, py).apply()
                    imageView!!.setBackgroundColor(0x00000000)
                }
                view = null
            }.create()
        val win: Window? = dialog.window
        win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        win.setDimAmount(0f)
        dialog.show()
        val windowLayoutParams: WindowManager.LayoutParams = win.attributes
        if (metrics.heightPixels > metrics.widthPixels) {
            windowLayoutParams.width = (metrics.widthPixels / 6) * 5
            windowLayoutParams.height = metrics.heightPixels / 2
        } else {
            windowLayoutParams.width = (metrics.heightPixels / 6) * 5
            windowLayoutParams.height = metrics.widthPixels / 2
        }
        win.attributes = windowLayoutParams
        if (imageView != null) imageView!!.setBackgroundColor(-0x10000)
    }

    companion object {
        private const val WIDTH: String = "ScreenLock_Width"
        private const val HEIGHT: String = "ScreenLock_Height"
        private const val POSITION_X: String = "ScreenLock_PositionX"
        private const val POSITION_Y: String = "ScreenLock_PositionY"
    }

    init {
        windowManager.defaultDisplay.getRealMetrics(metrics)
        width = sharedPreferences.getInt(WIDTH, metrics.widthPixels)
        height = sharedPreferences.getInt(HEIGHT, metrics.widthPixels)
        px = sharedPreferences.getInt(POSITION_X, metrics.widthPixels - width)
        py = sharedPreferences.getInt(POSITION_Y, metrics.heightPixels - height)
    }
}