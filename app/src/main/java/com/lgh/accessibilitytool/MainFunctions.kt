package com.lgh.accessibilitytool

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Notification
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Vibrator
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.View.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.SeekBar.OnSeekBarChangeListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MainFunctions(private val service: AccessibilityService) {
    private var doublePress = false
    private var isReleaseUp = false
    private var isReleaseDown = false
    private var skipAdvertising = false
    private var record_message = false
    private var controlLightness = false
    private var controlLock = false
    private var control_music = false
    private var controlMusicOnlyLock = false
    private var is_state_change_a = false
    private var is_state_change_b = false
    private var is_state_change_c = false
    private var startUpTimeInMills: Long = 0
    private var star_down: Long = 0
    private var win_state_count = 0
    private var vibration_strength = 0
    var handler: Handler? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var futureV: ScheduledFuture<*>? = null
    private var futureA: ScheduledFuture<*>? = null
    private var futureB: ScheduledFuture<*>? = null
    private lateinit var executorService: ScheduledExecutorService
    private lateinit var audioManager: AudioManager
    private lateinit var packageManager: PackageManager
    private lateinit var vibrator: Vibrator
    private var pac_msg: MutableSet<String?>? = null
    private lateinit var launchPackageSet: MutableSet<String>
    private lateinit var whitePackageList: MutableSet<String>
    private lateinit var homePackageList: MutableSet<String>
    private lateinit var removePackageList: MutableSet<String>
    private var keyWordList: ArrayList<String>? = null
    private var act_position: MutableMap<String?, SkipPositionDescribe>? = null
    private var act_widget: MutableMap<String?, MutableSet<WidgetButtonDescribe>>? = null
    private lateinit var accessibilityServiceInfo: AccessibilityServiceInfo
    private var cur_pac: String? = null
    private var cur_act: String? = null
    private var savePath: String? = null
    private lateinit var packageName: String
    private var windowManager: WindowManager? = null
    private var devicePolicyManager: DevicePolicyManager? = null
    private var screenLightness: ScreenLightness? = null
    private var simulateMediaButton: SimulateMediaButton? = null
    private var screenLock: ScreenLock? = null
    private var packageChangeReceiver: PackageChangeReceiver? = null
    private var screenOnReceiver: ScreenOffReceiver? = null
    private var widgetSet: Set<WidgetButtonDescribe>? = null
    private var aParams: WindowManager.LayoutParams? = null
    private var bParams: WindowManager.LayoutParams? = null
    private var cParams: WindowManager.LayoutParams? = null
    private var adView: View? = null
    private var windowLayout: View? = null
    private var imageView: ImageView? = null
    fun onServiceConnected() {
        try {
            isReleaseUp = true
            isReleaseDown = true
            doublePress = false
            cur_pac = "Initialize PackageName"
            cur_act = "Initialize ClassName"
            packageName = service.packageName
            audioManager =
                service.getSystemService(AccessibilityService.AUDIO_SERVICE) as AudioManager
            vibrator = service.getSystemService(AccessibilityService.VIBRATOR_SERVICE) as Vibrator
            sharedPreferences =
                service.getSharedPreferences(packageName, AccessibilityService.MODE_PRIVATE)
            windowManager =
                service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
            devicePolicyManager =
                service.getSystemService(AccessibilityService.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            packageManager = service.packageManager
            accessibilityServiceInfo = service.serviceInfo
            executorService = Executors.newSingleThreadScheduledExecutor()
            simulateMediaButton = SimulateMediaButton(service)
            screenLightness = ScreenLightness(service)
            screenLock = ScreenLock(service)
            packageChangeReceiver = PackageChangeReceiver()
            screenOnReceiver = ScreenOffReceiver()
            vibration_strength = sharedPreferences.getInt(VIBRATION_STRENGTH, 50)
            pac_msg = sharedPreferences.getStringSet(PAC_MSG, HashSet())
            whitePackageList = sharedPreferences.getStringSet(PAC_WHITE, mutableSetOf<String>()) as MutableSet<String>
            skipAdvertising = sharedPreferences.getBoolean(SKIP_ADVERTISING, true)
            control_music = sharedPreferences.getBoolean(CONTROL_MUSIC, true)
            record_message = sharedPreferences.getBoolean(RECORD_MESSAGE, false)
            controlLightness = sharedPreferences.getBoolean(CONTROL_LIGHTNESS, false)
            controlLock = sharedPreferences.getBoolean(
                CONTROL_LOCK,
                true
            ) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || devicePolicyManager!!.isAdminActive(
                ComponentName(
                    service, DeviceAdminReceiver::class.java
                )
            ))
            controlMusicOnlyLock = sharedPreferences.getBoolean(CONTROL_MUSIC_ONLY_LOCK, false)
            updatePackage()
            val packageChangeIntent = IntentFilter()
            packageChangeIntent.addAction(Intent.ACTION_PACKAGE_ADDED)
            packageChangeIntent.addAction(Intent.ACTION_PACKAGE_REMOVED)
            packageChangeIntent.addDataScheme("package")
            service.registerReceiver(packageChangeReceiver, packageChangeIntent)
            val screenIntent = IntentFilter()
            screenIntent.addAction(Intent.ACTION_SCREEN_ON)
            screenIntent.addAction(Intent.ACTION_SCREEN_OFF)
            service.registerReceiver(screenOnReceiver, screenIntent)
            savePath = service.externalCacheDir!!.absolutePath
            if (skipAdvertising) {
                (accessibilityServiceInfo.eventTypes or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).also {
                    accessibilityServiceInfo.eventTypes = it
                }
            }
            if (control_music && !controlMusicOnlyLock) {
                accessibilityServiceInfo.flags =
                    accessibilityServiceInfo.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            }
            if (record_message) {
                accessibilityServiceInfo.eventTypes =
                    accessibilityServiceInfo.eventTypes or AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
            }
            if (controlLightness) {
                screenLightness!!.showFloat()
            }
            if (controlLock) {
                screenLock!!.showLockFloat()
            }
            service.serviceInfo = accessibilityServiceInfo
            val aJson = sharedPreferences.getString(ACTIVITY_WIDGET, null)
            act_widget = if (aJson != null) {
                val type =
                    object : TypeToken<TreeMap<String?, Set<WidgetButtonDescribe?>?>?>() {}.type
                Gson().fromJson(aJson, type)
            } else {
                TreeMap()
            }
            val bJson = sharedPreferences.getString(ACTIVITY_POSITION, null)
            act_position = if (bJson != null) {
                val type = object : TypeToken<TreeMap<String?, SkipPositionDescribe?>?>() {}.type
                Gson().fromJson(bJson, type)
            } else {
                TreeMap()
            }
            val cJson = sharedPreferences.getString(KEY_WORD_LIST, null)
            if (cJson != null) {
                val type = object : TypeToken<ArrayList<String?>?>() {}.type
                keyWordList = Gson().fromJson(cJson, type)
            } else {
                keyWordList = ArrayList()
                keyWordList!!.add("跳过")
            }
            futureB = executorService.schedule(Runnable { }, 0, TimeUnit.MILLISECONDS)
            futureA = futureB
            futureV = futureA
            handler = Handler { msg ->
                when (msg.what) {
                    0x00 -> mainUI()
                    0x01 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                    }
                    0x02 -> updatePackage()
                    0x03 -> {
                        cur_pac = "ScreenOff PackageName"
                        if (control_music && controlMusicOnlyLock) {
                            accessibilityServiceInfo.flags =
                                accessibilityServiceInfo.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                            service.serviceInfo = accessibilityServiceInfo
                        }
                    }
                    0x04 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        service.disableSelf()
                    }
                    0x05 -> if (control_music && controlMusicOnlyLock) {
                        accessibilityServiceInfo.flags =
                            accessibilityServiceInfo.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
                        service.serviceInfo = accessibilityServiceInfo
                    }
                }
                true
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val root = service.rootInActiveWindow
                    val temPackage = event.packageName
                    val temClass = event.className
                    val packageName = root?.packageName?.toString() ?: temPackage?.toString()
                    val activityName = temClass?.toString()
                    if (packageName != null) {
                        if (packageName != cur_pac) {
                            if (launchPackageSet.contains(packageName)) {
                                cur_pac = packageName
                                futureA!!.cancel(false)
                                futureB!!.cancel(false)
                                accessibilityServiceInfo.eventTypes =
                                    accessibilityServiceInfo.eventTypes or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                                service.serviceInfo = accessibilityServiceInfo
                                is_state_change_a = true
                                is_state_change_b = true
                                is_state_change_c = true
                                win_state_count = 0
                                widgetSet = null
                                futureA = executorService!!.schedule({
                                    is_state_change_a = false
                                    is_state_change_c = false
                                }, 8000, TimeUnit.MILLISECONDS)
                                futureB = executorService!!.schedule({
                                    accessibilityServiceInfo!!.eventTypes =
                                        accessibilityServiceInfo!!.eventTypes and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED.inv()
                                    service.serviceInfo = accessibilityServiceInfo
                                    is_state_change_b = false
                                    widgetSet = null
                                }, 30000, TimeUnit.MILLISECONDS)
                            } else if (whitePackageList!!.contains(packageName)) {
                                cur_pac = packageName
                                if (is_state_change_a || is_state_change_b || is_state_change_c) {
                                    closeContentChanged()
                                }
                            }
                        }
                    }
                    if (activityName != null) {
                        if (!activityName.startsWith("android.widget.") && !activityName.startsWith(
                                "android.view."
                            )
                        ) {
                            cur_act = activityName
                            if (is_state_change_a) {
                                val skipPositionDescribe = act_position!![activityName]
                                if (skipPositionDescribe != null) {
                                    is_state_change_a = false
                                    is_state_change_c = false
                                    futureA!!.cancel(false)
                                    executorService!!.scheduleAtFixedRate(
                                        object : Runnable {
                                            var num = 0
                                            override fun run() {
                                                if (num < skipPositionDescribe.number && cur_act == skipPositionDescribe.activityName) {
                                                    click(
                                                        skipPositionDescribe.x,
                                                        skipPositionDescribe.y,
                                                        0,
                                                        20
                                                    )
                                                    num++
                                                } else {
                                                    throw RuntimeException()
                                                }
                                            }
                                        },
                                        skipPositionDescribe.delay.toLong(),
                                        skipPositionDescribe.period.toLong(),
                                        TimeUnit.MILLISECONDS
                                    )
                                }
                            }
                            if (is_state_change_b) {
                                widgetSet = act_widget!![activityName]
                            }
                        }
                    }
                    if (packageName != null && packageName == cur_pac) {
                        if (is_state_change_b && widgetSet != null) {
                            findSkipButtonByWidget(root, widgetSet!!)
                        }
                        if (is_state_change_c) {
                            findSkipButtonByText(root)
                        }
                    }
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> if (event.packageName == cur_pac) {
                    if (is_state_change_b && widgetSet != null) {
                        findSkipButtonByWidget(event.source, widgetSet!!)
                    }
                    if (is_state_change_c) {
                        findSkipButtonByText(event.source)
                    }
                    if (win_state_count++ >= 150) {
                        closeContentChanged()
                    }
                }
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> if (event.parcelableData is Notification && pac_msg!!.contains(
                        event.packageName
                    )
                ) {
                    val list_msg = event.text
                    val builder = StringBuilder()
                    for (s in list_msg) {
                        builder.append(s.toString().replace("\\s".toRegex(), ""))
                    }
                    val tem = builder.toString()
                    if (!tem.isEmpty()) {
                        val writer = FileWriter("$savePath/NotificationMessageCache.txt", true)
                        writer.append("[")
                        writer.append(tem)
                        writer.append(
                            """]
"""
                        )
                        writer.close()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        return try {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            startUpTimeInMills = System.currentTimeMillis()
                            isReleaseUp = false
                            doublePress = false
                            if (isReleaseDown) {
                                futureV =
                                    executorService.schedule({ //                                        Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_UP -> THREAD");
                                        if (!isReleaseDown) {
                                            simulateMediaButton!!.sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                            vibrator!!.vibrate(vibration_strength.toLong())
                                        } else if (!isReleaseUp && audioManager!!.isMusicActive) {
                                            simulateMediaButton!!.sendMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT)
                                            vibrator!!.vibrate(vibration_strength.toLong())
                                        }
                                    }, 800, TimeUnit.MILLISECONDS)
                            } else {
                                doublePress = true
                            }
                        }
                        KeyEvent.ACTION_UP -> {
                            futureV!!.cancel(false)
                            isReleaseUp = true
                            if (!doublePress && System.currentTimeMillis() - startUpTimeInMills < 800) {
                                audioManager!!.adjustVolume(
                                    AudioManager.ADJUST_RAISE,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            }
                        }
                    }
                    true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            //                            Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_DOWN -> KeyEvent.ACTION_DOWN");
                            star_down = System.currentTimeMillis()
                            isReleaseDown = false
                            doublePress = false
                            if (isReleaseUp) {
                                futureV =
                                    executorService.schedule({ //                                        Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_DOWN -> THREAD");
                                        if (!isReleaseUp) {
                                            simulateMediaButton!!.sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                            vibrator!!.vibrate(vibration_strength.toLong())
                                        } else if (!isReleaseDown && audioManager!!.isMusicActive) {
                                            simulateMediaButton!!.sendMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                            vibrator!!.vibrate(vibration_strength.toLong())
                                        }
                                    }, 800, TimeUnit.MILLISECONDS)
                            } else {
                                doublePress = true
                            }
                        }
                        KeyEvent.ACTION_UP -> {
                            //                            Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_DOWN -> KeyEvent.ACTION_UP");
                            futureV!!.cancel(false)
                            isReleaseDown = true
                            if (!doublePress && System.currentTimeMillis() - star_down < 800) {
                                audioManager!!.adjustVolume(
                                    AudioManager.ADJUST_LOWER,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            }
                        }
                    }
                    true
                }
                else -> //                    Log.i(TAG,KeyEvent.keyCodeToString(event.getKeyCode()));
                    false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        try {
            if (controlLightness) {
                screenLightness!!.refreshOnOrientationChange()
            }
            if (controlLock) {
                when (newConfig.orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> screenLock!!.showLockFloat()
                    Configuration.ORIENTATION_LANDSCAPE -> screenLock!!.dismiss()
                }
            }
            if (adView != null && imageView != null && windowLayout != null) {
                val metrics = DisplayMetrics()
                windowManager!!.defaultDisplay.getRealMetrics(metrics)
                cParams!!.x = (metrics.widthPixels - cParams!!.width) / 2
                cParams!!.y = (metrics.heightPixels - cParams!!.height) / 2
                aParams!!.x = (metrics.widthPixels - aParams!!.width) / 2
                aParams!!.y = metrics.heightPixels - aParams!!.height
                windowManager!!.updateViewLayout(adView, aParams)
                windowManager!!.updateViewLayout(imageView, cParams)
                val layout = windowLayout!!.findViewById<FrameLayout>(R.id.frame)
                layout.removeAllViews()
                val text = TextView(service)
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                text.setTextColor(-0x10000)
                text.text = "请重新刷新布局"
                layout.addView(
                    text,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun onUnbind(intent: Intent?) {
        try {
            service.unregisterReceiver(packageChangeReceiver)
            service.unregisterReceiver(screenOnReceiver)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 自动查找启动广告的
     * “跳过”的控件
     */
    private fun findSkipButtonByText(nodeInfo: AccessibilityNodeInfo?) {
        if (nodeInfo == null) return
        for (n in keyWordList!!.indices) {
            val list = nodeInfo.findAccessibilityNodeInfosByText(
                keyWordList!![n]
            )
            if (!list.isEmpty()) {
                for (e: AccessibilityNodeInfo in list) {
                    if (!e.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        if (!e.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            val rect = Rect()
                            e.getBoundsInScreen(rect)
                            click(rect.centerX(), rect.centerY(), 0, 20)
                        }
                    }
                    e.recycle()
                }
                is_state_change_c = false
                return
            }
        }
        nodeInfo.recycle()
    }

    /**
     * 查找并点击由
     * WidgetButtonDescribe
     * 定义的控件
     */
    private fun findSkipButtonByWidget(
        root: AccessibilityNodeInfo?,
        set: Set<WidgetButtonDescribe>
    ) {
        var a = 0
        var b = 1
        var listA = ArrayList<AccessibilityNodeInfo?>()
        var listB = ArrayList<AccessibilityNodeInfo?>()
        listA.add(root)
        while (a < b) {
            val node = listA[a++]
            if (node != null) {
                val temRect = Rect()
                node.getBoundsInScreen(temRect)
                val cId: CharSequence? = node.viewIdResourceName
                val cDescribe = node.contentDescription
                val cText = node.text
                for (e in set) {
                    var isFind = false
                    if (temRect == e.bonus) {
                        isFind = true
                    } else if (cId != null && !e.idName.isEmpty() && cId.toString() == e.idName) {
                        isFind = true
                    } else if (cDescribe != null && !e.describe.isEmpty() && cDescribe.toString()
                            .contains(e.describe)
                    ) {
                        isFind = true
                    } else if (cText != null && !e.text.isEmpty() && cText.toString()
                            .contains(e.text)
                    ) {
                        isFind = true
                    }
                    if (isFind) {
                        if (e.onlyClick) {
                            click(temRect.centerX(), temRect.centerY(), 0, 20)
                        } else {
                            if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                if (!node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    click(temRect.centerX(), temRect.centerY(), 0, 20)
                                }
                            }
                        }
                        widgetSet = null
                        return
                    }
                }
                for (n in 0 until node.childCount) {
                    listB.add(node.getChild(n))
                }
                node.recycle()
            }
            if (a == b) {
                a = 0
                b = listB.size
                listA = listB
                listB = ArrayList()
            }
        }
    }

    /**
     * 查找所有
     * 的控件
     */
    private fun findAllNode(
        roots: List<AccessibilityNodeInfo>,
        list: MutableList<AccessibilityNodeInfo>
    ) {
        try {
            val tem = ArrayList<AccessibilityNodeInfo>()
            for (e in roots) {
                if (e == null) continue
                val rect = Rect()
                e.getBoundsInScreen(rect)
                if (rect.width() <= 0 || rect.height() <= 0) continue
                list.add(e)
                for (n in 0 until e.childCount) {
                    tem.add(e.getChild(n))
                }
            }
            if (!tem.isEmpty()) {
                findAllNode(tem, list)
            }
        } catch (e: Throwable) {
//            e.printStackTrace();
        }
    }

    /**
     * 模拟
     * 点击
     */
    private fun click(X: Int, Y: Int, start_time: Long, duration: Long): Boolean {
        val path = Path()
        path.moveTo(X.toFloat(), Y.toFloat())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder =
                GestureDescription.Builder()
                    .addStroke(StrokeDescription(path, start_time, duration))
            service.dispatchGesture(builder.build(), null, null)
        } else {
            false
        }
    }

    /**
     * 关闭
     * ContentChanged
     * 事件的响应
     */
    private fun closeContentChanged() {
        accessibilityServiceInfo!!.eventTypes =
            accessibilityServiceInfo!!.eventTypes and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED.inv()
        service.serviceInfo = accessibilityServiceInfo
        is_state_change_a = false
        is_state_change_b = false
        is_state_change_c = false
        widgetSet = null
        futureA!!.cancel(false)
        futureB!!.cancel(false)
    }

    /**
     * 在安装卸载软件时触发调用，
     * 更新相关包名的集合
     */
    private fun updatePackage() {
        launchPackageSet = HashSet()
        homePackageList = HashSet()
        removePackageList = HashSet()
        val systemPackageSet: MutableSet<String> = HashSet()
        var intent: Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        var resolveInfoList: List<ResolveInfo> =
            packageManager!!.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            launchPackageSet.add(e.activityInfo.packageName)
            if (e.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM) {
                systemPackageSet.add(e.activityInfo.packageName)
            }
        }
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        resolveInfoList = packageManager!!.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            homePackageList.add(e.activityInfo.packageName)
        }
        val inputMethodInfoList =
            (service.getSystemService(AccessibilityService.INPUT_METHOD_SERVICE) as InputMethodManager).inputMethodList
        for (e in inputMethodInfoList) {
            removePackageList.add(e.packageName)
        }
        if (whitePackageList.retainAll(launchPackageSet)) {
            sharedPreferences.edit().putStringSet(PAC_WHITE, whitePackageList).apply()
        }
        if (pac_msg!!.retainAll(launchPackageSet)) {
            sharedPreferences.edit().putStringSet(PAC_MSG, pac_msg).apply()
        }
        removePackageList.add(packageName)
        removePackageList.add("com.android.systemui")
        whitePackageList.removeAll(removePackageList)
        whitePackageList.addAll(homePackageList)
        whitePackageList.add("com.android.packageinstaller")
        launchPackageSet.removeAll(whitePackageList!!)
        launchPackageSet.removeAll(removePackageList)
    }

    /**
     * 用于设置的主要UI界面
     */
    private fun mainUI() {
        val metrics = DisplayMetrics()
        windowManager!!.defaultDisplay.getRealMetrics(metrics)
        val componentName = ComponentName(service, DeviceAdminReceiver::class.java)
        val b = metrics.heightPixels > metrics.widthPixels
        val width = if (b) metrics.widthPixels else metrics.heightPixels
        val height = if (b) metrics.heightPixels else metrics.widthPixels
        val inflater = LayoutInflater.from(service)
        val view_main = inflater.inflate(R.layout.main_dialog, null)
        val dialog_main =
            AlertDialog.Builder(service).setTitle(R.string.simple_name).setIcon(R.drawable.a)
                .setCancelable(false).setView(view_main).create()
        val switch_skip_advertising = view_main.findViewById<Switch>(R.id.skip_advertising)
        val switch_music_control = view_main.findViewById<Switch>(R.id.music_control)
        val switch_record_message = view_main.findViewById<Switch>(R.id.record_message)
        val switch_screen_lightness = view_main.findViewById<Switch>(R.id.screen_lightness)
        val switchScreenLock = view_main.findViewById<Switch>(R.id.screen_lock)
        val btSetting = view_main.findViewById<TextView>(R.id.set)
        val bt_look = view_main.findViewById<TextView>(R.id.look)
        val bt_cancel = view_main.findViewById<TextView>(R.id.cancel)
        val bt_sure = view_main.findViewById<TextView>(R.id.sure)
        switch_skip_advertising.isChecked = skipAdvertising
        switch_music_control.isChecked = control_music
        switch_record_message.isChecked = record_message
        switch_screen_lightness.isChecked = controlLightness
        switchScreenLock.isChecked =
            controlLock && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || devicePolicyManager!!.isAdminActive(
                componentName
            ))
        switch_skip_advertising.setOnLongClickListener(object : OnLongClickListener {
            override fun onLongClick(v: View): Boolean {
                val view = inflater.inflate(R.layout.skipdesc_parent, null)
                val dialog_adv = AlertDialog.Builder(service).setView(view).create()
                val parentView = view.findViewById<LinearLayout>(R.id.skip_desc)
                val addButton = view.findViewById<Button>(R.id.add)
                val chooseButton = view.findViewById<Button>(R.id.choose)
                val keyButton = view.findViewById<Button>(R.id.keyword)
                val set_position: Set<Map.Entry<String?, SkipPositionDescribe>> =
                    act_position!!.entries
                for (e: Map.Entry<String?, SkipPositionDescribe> in set_position) {
                    val childView = inflater.inflate(R.layout.position_a, null)
                    val imageView = childView.findViewById<ImageView>(R.id.img)
                    val className = childView.findViewById<TextView>(R.id.classname)
                    val x = childView.findViewById<EditText>(R.id.x)
                    val y = childView.findViewById<EditText>(R.id.y)
                    val delay = childView.findViewById<EditText>(R.id.delay)
                    val period = childView.findViewById<EditText>(R.id.period)
                    val number = childView.findViewById<EditText>(R.id.number)
                    val modify = childView.findViewById<TextView>(R.id.modify)
                    val delete = childView.findViewById<TextView>(R.id.delete)
                    val sure = childView.findViewById<TextView>(R.id.sure)
                    val value = e.value
                    try {
                        imageView.setImageDrawable(packageManager!!.getApplicationIcon(value.packageName))
                    } catch (e1: PackageManager.NameNotFoundException) {
                        imageView.setImageResource(R.drawable.u)
                        modify.text = "该应用未安装"
                    }
                    className.text = value.activityName
                    x.setText(value.x.toString())
                    y.setText(value.y.toString())
                    delay.setText(value.delay.toString())
                    period.setText(value.period.toString())
                    number.setText(value.number.toString())
                    delete.setOnClickListener(View.OnClickListener {
                        act_position!!.remove(value.activityName)
                        parentView.removeView(childView)
                    })
                    sure.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View) {
                            val sX = x.text.toString()
                            val sY = y.text.toString()
                            val sDelay = delay.text.toString()
                            val sPeriod = period.text.toString()
                            val sNumber = number.text.toString()
                            modify.setTextColor(-0x10000)
                            if (sX.isEmpty() || sY.isEmpty() || sPeriod.isEmpty() || sNumber.isEmpty()) {
                                modify.text = "内容不能为空"
                                return
                            } else if (Integer.valueOf(sX) < 0 || Integer.valueOf(sX) > metrics.widthPixels) {
                                modify.text = "X坐标超出屏幕寸"
                                return
                            } else if (Integer.valueOf(sY) < 0 || Integer.valueOf(sY) > metrics.heightPixels) {
                                modify.text = "Y坐标超出屏幕寸"
                                return
                            } else if (Integer.valueOf(sDelay) < 0 || Integer.valueOf(sDelay) > 4000) {
                                modify.text = "点击延迟为0~4000(ms)之间"
                                return
                            } else if (Integer.valueOf(sPeriod) < 100 || Integer.valueOf(sPeriod) > 2000) {
                                modify.text = "点击间隔应为100~2000(ms)之间"
                                return
                            } else if (Integer.valueOf(sNumber) < 1 || Integer.valueOf(sNumber) > 20) {
                                modify.text = "点击次数应为1~20次之间"
                                return
                            } else {
                                value.x = Integer.valueOf(sX)
                                value.y = Integer.valueOf(sY)
                                value.delay = Integer.valueOf(sDelay)
                                value.period = Integer.valueOf(sPeriod)
                                value.number = Integer.valueOf(sNumber)
                                modify.text = SimpleDateFormat("HH:mm:ss a", Locale.ENGLISH).format(
                                    Date()
                                ) + "(修改成功)"
                                modify.setTextColor(-0x1000000)
                            }
                        }
                    })
                    parentView.addView(childView)
                }
                val set_key: Set<String?> = act_widget!!.keys
                for (e: String? in set_key) {
                    val widgets = (act_widget!![e])!!
                    for (widget: WidgetButtonDescribe in widgets) {
                        val childView = inflater.inflate(R.layout.widget_a, null)
                        val imageView = childView.findViewById<ImageView>(R.id.img)
                        val className = childView.findViewById<TextView>(R.id.classname)
                        val widgetClickable =
                            childView.findViewById<EditText>(R.id.widget_clickable)
                        val widgetBonus = childView.findViewById<EditText>(R.id.widget_bonus)
                        val widgetId = childView.findViewById<EditText>(R.id.widget_id)
                        val widgetDescribe = childView.findViewById<EditText>(R.id.widget_describe)
                        val widgetText = childView.findViewById<EditText>(R.id.widget_text)
                        val onlyClick = childView.findViewById<Switch>(R.id.widget_onlyClick)
                        val modify = childView.findViewById<TextView>(R.id.modify)
                        val delete = childView.findViewById<TextView>(R.id.delete)
                        val sure = childView.findViewById<TextView>(R.id.sure)
                        try {
                            imageView.setImageDrawable(packageManager!!.getApplicationIcon(widget.packageName))
                        } catch (notFound: PackageManager.NameNotFoundException) {
                            imageView.setImageResource(R.drawable.u)
                            modify.text = "该应用未安装"
                        }
                        className.text = widget.activityName
                        widgetClickable.setText(if (widget.clickable) "true" else "false")
                        widgetBonus.setText(widget.bonus.toShortString())
                        widgetId.setText(widget.idName)
                        widgetDescribe.setText(widget.describe)
                        widgetText.setText(widget.text)
                        onlyClick.isChecked = widget.onlyClick
                        parentView.addView(childView)
                        delete.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                widgets.remove(widget)
                                parentView.removeView(childView)
                                if (widgets.isEmpty()) {
                                    act_widget!!.remove(widget.activityName)
                                }
                            }
                        })
                        sure.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                widget.idName = widgetId.text.toString().trim { it <= ' ' }
                                widget.describe = widgetDescribe.text.toString().trim { it <= ' ' }
                                widget.text = widgetText.text.toString().trim { it <= ' ' }
                                widget.onlyClick = onlyClick.isChecked
                                widgetId.setText(widget.idName)
                                widgetDescribe.setText(widget.describe)
                                widgetText.setText(widget.text)
                                modify.text = SimpleDateFormat("HH:mm:ss a", Locale.ENGLISH).format(
                                    Date()
                                ) + "(修改成功)"
                            }
                        })
                    }
                }
                chooseButton.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        val view = inflater.inflate(R.layout.view_select, null)
                        val listView = view.findViewById<ListView>(R.id.listView)
                        val listApp = ArrayList<AppInformation>()
                        val list: MutableList<String?> = ArrayList()
                        list.addAll((whitePackageList))
                        list.addAll((launchPackageSet))
                        list.removeAll((homePackageList))
                        for (e: String? in list) {
                            try {
                                val info = packageManager!!.getApplicationInfo(
                                    (e)!!,
                                    PackageManager.GET_META_DATA
                                )
                                listApp.add(
                                    AppInformation(
                                        e,
                                        packageManager!!.getApplicationLabel(info).toString(),
                                        packageManager!!.getApplicationIcon(info)
                                    )
                                )
                            } catch (nfe: PackageManager.NameNotFoundException) {
                            }
                        }
                        val baseAdapter: BaseAdapter = object : BaseAdapter() {
                            override fun getCount(): Int {
                                return listApp.size
                            }

                            override fun getItem(position: Int): Any {
                                return position
                            }

                            override fun getItemId(position: Int): Long {
                                return position.toLong()
                            }

                            override fun getView(
                                position: Int,
                                convertView: View,
                                parent: ViewGroup
                            ): View {
                                var convertView = convertView
                                val holder: ViewHolder
                                if (convertView == null) {
                                    convertView = inflater.inflate(R.layout.view_pac, null)
                                    holder = ViewHolder(convertView)
                                    convertView.tag = holder
                                } else {
                                    holder = convertView.tag as ViewHolder
                                }
                                val tem = listApp[position]
                                holder.textView.text = tem.applicationName
                                holder.imageView.setImageDrawable(tem.applicationIcon)
                                holder.checkBox.isChecked =
                                    whitePackageList.contains(tem.packageName)
                                return convertView
                            }
                        }
                        listView.onItemClickListener =
                            OnItemClickListener { parent, view, position, id ->
                                val c = (view.tag as ViewHolder).checkBox
                                val str = listApp[position].packageName
                                if (c.isChecked) {
                                    whitePackageList.remove(str)
                                    launchPackageSet.add(str)
                                    c.isChecked = false
                                } else {
                                    whitePackageList.add(str)
                                    launchPackageSet.remove(str)
                                    c.isChecked = true
                                }
                            }
                        listView.adapter = baseAdapter
                        val dialog_pac = AlertDialog.Builder(service).setView(view)
                            .setOnDismissListener(object : DialogInterface.OnDismissListener {
                                override fun onDismiss(dialog: DialogInterface) {
                                    sharedPreferences!!.edit()
                                        .putStringSet(PAC_WHITE, whitePackageList)
                                        .apply()
                                }
                            }).create()
                        val win = dialog_pac.window
                        win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
                        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                        win.setDimAmount(0f)
                        dialog_pac.show()
                        val params = win.attributes
                        params.width = (width / 6) * 5
                        win.attributes = params
                        dialog_adv.dismiss()
                    }

                    inner class AppInformation(
                        var packageName: String,
                        var applicationName: String,
                        var applicationIcon: Drawable
                    ) {
                    }

                    inner class ViewHolder(v: View) {
                        var textView: TextView
                        var imageView: ImageView
                        var checkBox: CheckBox

                        init {
                            textView = v.findViewById(R.id.name)
                            imageView = v.findViewById(R.id.img)
                            checkBox = v.findViewById(R.id.check)
                        }
                    }
                })
                addButton.setOnClickListener(object : View.OnClickListener {
                    var widgetDescribe: WidgetButtonDescribe? = null
                    var positionDescribe: SkipPositionDescribe? = null

                    @SuppressLint("ClickableViewAccessibility")
                    override fun onClick(v: View) {
                        if ((imageView != null) || (adView != null) || (windowLayout != null)) {
                            dialog_adv.dismiss()
                            return
                        }
                        widgetDescribe = WidgetButtonDescribe()
                        positionDescribe = SkipPositionDescribe("", "", 0, 0, 500, 500, 1)
                        adView = inflater.inflate(R.layout.advertise_desc, null)
                        val pacName = adView?.findViewById<TextView>(R.id.pacName)
                        val actName = adView?.findViewById<TextView>(R.id.actName)
                        val widget = adView?.findViewById<TextView>(R.id.widget)
                        val xyP = adView?.findViewById<TextView>(R.id.xy)
                        val switchWid = adView?.findViewById<Button>(R.id.switch_wid)
                        val saveWidgetButton = adView?.findViewById<Button>(R.id.save_wid)
                        val switchAim = adView?.findViewById<Button>(R.id.switch_aim)
                        val savePositionButton = adView?.findViewById<Button>(R.id.save_aim)
                        val quitButton = adView?.findViewById<Button>(R.id.quit)
                        windowLayout = inflater.inflate(R.layout.accessibilitynode_desc, null)
                        val layout_add = windowLayout?.findViewById<FrameLayout>(R.id.frame)
                        imageView = ImageView(service)
                        imageView!!.setImageResource(R.drawable.p)
                        aParams = WindowManager.LayoutParams()
                        aParams!!.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                        aParams!!.format = PixelFormat.TRANSPARENT
                        aParams!!.gravity = Gravity.START or Gravity.TOP
                        aParams!!.flags =
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        aParams!!.width = width
                        aParams!!.height = height / 5
                        aParams!!.x = (metrics.widthPixels - aParams!!.width) / 2
                        aParams!!.y = metrics.heightPixels - aParams!!.height
                        aParams!!.alpha = 0.8f
                        bParams = WindowManager.LayoutParams()
                        bParams!!.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                        bParams!!.format = PixelFormat.TRANSPARENT
                        bParams!!.gravity = Gravity.START or Gravity.TOP
                        bParams!!.width = metrics.widthPixels
                        bParams!!.height = metrics.heightPixels
                        bParams!!.flags =
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        bParams!!.alpha = 0f
                        cParams = WindowManager.LayoutParams()
                        cParams!!.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                        cParams!!.format = PixelFormat.TRANSPARENT
                        cParams!!.flags =
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        cParams!!.gravity = Gravity.START or Gravity.TOP
                        cParams!!.height = width / 4
                        cParams!!.width = cParams!!.height
                        cParams!!.x = (metrics.widthPixels - cParams!!.width) / 2
                        cParams!!.y = (metrics.heightPixels - cParams!!.height) / 2
                        cParams!!.alpha = 0f
                        adView?.setOnTouchListener(object : OnTouchListener {
                            var x = 0
                            var y = 0
                            override fun onTouch(v: View, event: MotionEvent): Boolean {
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        x = event.rawX.roundToInt()
                                        y = event.rawY.roundToInt()
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        aParams!!.x = (aParams!!.x + (event.rawX - x)).roundToInt()
                                        aParams!!.y = (aParams!!.y + (event.rawY - y)).roundToInt()
                                        x = event.rawX.roundToInt()
                                        y = event.rawY.roundToInt()
                                        windowManager!!.updateViewLayout(adView, aParams)
                                    }
                                }
                                return true
                            }
                        })
                        imageView!!.setOnTouchListener(object : OnTouchListener {
                            var x = 0
                            var y = 0
                            var width = cParams!!.width / 2
                            var height = cParams!!.height / 2
                            override fun onTouch(v: View, event: MotionEvent): Boolean {
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        savePositionButton?.isEnabled = true
                                        cParams!!.alpha = 0.9f
                                        windowManager!!.updateViewLayout(imageView, cParams)
                                        x = event.rawX.roundToInt()
                                        y = event.rawY.roundToInt()
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        cParams!!.x = (cParams!!.x + (event.rawX - x)).roundToInt()
                                        cParams!!.y = (cParams!!.y + (event.rawY - y)).roundToInt()
                                        x = event.rawX.roundToInt()
                                        y = event.rawY.roundToInt()
                                        windowManager!!.updateViewLayout(imageView, cParams)
                                        positionDescribe!!.packageName = (cur_pac)!!
                                        positionDescribe!!.activityName = (cur_act)!!
                                        positionDescribe!!.x = cParams!!.x + width
                                        positionDescribe!!.y = cParams!!.y + height
                                        pacName?.text = positionDescribe!!.packageName
                                        actName?.text = positionDescribe!!.activityName
                                        xyP?.text =
                                            "X轴：" + positionDescribe!!.x + "    " + "Y轴：" + positionDescribe!!.y + "    " + "(其他参数默认)"
                                    }
                                    MotionEvent.ACTION_UP -> {
                                        cParams!!.alpha = 0.5f
                                        windowManager!!.updateViewLayout(imageView, cParams)
                                    }
                                }
                                return true
                            }
                        })
                        switchWid?.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                val button = v as Button
                                if (bParams!!.alpha == 0f) {
                                    val root = service.rootInActiveWindow ?: return
                                    widgetDescribe!!.packageName = (cur_pac)!!
                                    widgetDescribe!!.activityName = (cur_act)!!
                                    layout_add?.removeAllViews()
                                    val roots = ArrayList<AccessibilityNodeInfo>()
                                    roots.add(root)
                                    val nodeList = ArrayList<AccessibilityNodeInfo>()
                                    findAllNode(roots, nodeList)
                                    Collections.sort(
                                        nodeList
                                    ) { a, b ->
                                        val rectA = Rect()
                                        val rectB = Rect()
                                        a.getBoundsInScreen(rectA)
                                        b.getBoundsInScreen(rectB)
                                        rectB.width() * rectB.height() - rectA.width() * rectA.height()
                                    }
                                    for (e: AccessibilityNodeInfo in nodeList) {
                                        val temRect = Rect()
                                        e.getBoundsInScreen(temRect)
                                        val params = FrameLayout.LayoutParams(
                                            temRect.width(),
                                            temRect.height()
                                        )
                                        params.leftMargin = temRect.left
                                        params.topMargin = temRect.top
                                        val img = ImageView(
                                            service
                                        )
                                        img.setBackgroundResource(R.drawable.node)
                                        img.isFocusableInTouchMode = true
                                        img.setOnClickListener(object : View.OnClickListener {
                                            override fun onClick(v: View) {
                                                v.requestFocus()
                                            }
                                        })
                                        img.onFocusChangeListener = object : OnFocusChangeListener {
                                            override fun onFocusChange(v: View, hasFocus: Boolean) {
                                                if (hasFocus) {
                                                    widgetDescribe!!.bonus = temRect
                                                    widgetDescribe!!.clickable = e.isClickable
                                                    widgetDescribe!!.className =
                                                        e.className.toString()
                                                    val cId: CharSequence? = e.viewIdResourceName
                                                    widgetDescribe!!.idName = cId?.toString() ?: ""
                                                    val cDesc = e.contentDescription
                                                    widgetDescribe!!.describe =
                                                        cDesc?.toString() ?: ""
                                                    val cText = e.text
                                                    widgetDescribe!!.text = cText?.toString() ?: ""
                                                    saveWidgetButton?.isEnabled = true
                                                    pacName?.text = widgetDescribe!!.packageName
                                                    actName?.text = widgetDescribe!!.activityName
                                                    widget?.text =
                                                        "click:" + (if (e.isClickable) "true" else "false") + " " + "bonus:" + temRect.toShortString() + " " + "id:" + (cId?.toString()
                                                            ?.substring(
                                                                cId.toString().indexOf("id/") + 3
                                                            )
                                                            ?: "null") + " " + "desc:" + (cDesc?.toString()
                                                            ?: "null") + " " + "text:" + (cText?.toString()
                                                            ?: "null")
                                                    v.setBackgroundResource(R.drawable.node_focus)
                                                } else {
                                                    v.setBackgroundResource(R.drawable.node)
                                                }
                                            }
                                        }
                                        layout_add?.addView(img, params)
                                    }
                                    bParams!!.alpha = 0.5f
                                    bParams!!.flags =
                                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    windowManager!!.updateViewLayout(windowLayout, bParams)
                                    pacName?.text = widgetDescribe!!.packageName
                                    actName?.text = widgetDescribe!!.activityName
                                    button.text = "隐藏布局"
                                } else {
                                    bParams!!.alpha = 0f
                                    bParams!!.flags =
                                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    windowManager!!.updateViewLayout(windowLayout, bParams)
                                    saveWidgetButton?.isEnabled = false
                                    button.text = "显示布局"
                                }
                            }
                        })
                        switchAim?.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                val button = v as Button
                                if (cParams!!.alpha == 0f) {
                                    positionDescribe!!.packageName = (cur_pac)!!
                                    positionDescribe!!.activityName = (cur_act)!!
                                    cParams!!.alpha = 0.5f
                                    cParams!!.flags =
                                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    windowManager!!.updateViewLayout(imageView, cParams)
                                    pacName?.text = positionDescribe!!.packageName
                                    actName?.text = positionDescribe!!.activityName
                                    button.text = "隐藏准心"
                                } else {
                                    cParams!!.alpha = 0f
                                    cParams!!.flags =
                                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    windowManager!!.updateViewLayout(imageView, cParams)
                                    savePositionButton?.isEnabled = false
                                    button.text = "显示准心"
                                }
                            }
                        })
                        saveWidgetButton?.setOnClickListener {
                            val temWidget = WidgetButtonDescribe(
                                widgetDescribe!!
                            )
                            var set = act_widget!![widgetDescribe!!.activityName]
                            if (set == null) {
                                set = HashSet()
                                set.add(temWidget)
                                act_widget!![widgetDescribe!!.activityName] = set
                            } else {
                                set.add(temWidget)
                            }
                            saveWidgetButton.isEnabled = false
                            pacName?.text = widgetDescribe!!.packageName + " (以下控件数据已保存)"
                        }
                        savePositionButton?.setOnClickListener(OnClickListener {
                            act_position!![positionDescribe!!.activityName] =
                                SkipPositionDescribe(
                                    positionDescribe!!
                                )
                            savePositionButton.isEnabled = false
                            pacName?.text = positionDescribe!!.packageName + " (以下坐标数据已保存)"
                        })
                        quitButton?.setOnClickListener {
                            val gson = Gson()
                            sharedPreferences.edit()
                                .putString(ACTIVITY_POSITION, gson.toJson(act_position))
                                .putString(
                                    ACTIVITY_WIDGET, gson.toJson(act_widget)
                                ).apply()
                            windowManager!!.removeViewImmediate(windowLayout)
                            windowManager!!.removeViewImmediate(adView)
                            windowManager!!.removeViewImmediate(imageView)
                            windowLayout = null
                            adView = null
                            imageView = null
                            aParams = null
                            bParams = null
                            cParams = null
                        }
                        windowManager!!.addView(windowLayout, bParams)
                        windowManager!!.addView(adView, aParams)
                        windowManager!!.addView(imageView, cParams)
                        dialog_adv.dismiss()
                    }
                })
                keyButton.setOnClickListener {
                    val addKeyView = inflater.inflate(R.layout.add_keyword, null)
                    val layout = addKeyView.findViewById<LinearLayout>(R.id.keyList)
                    val edit = addKeyView.findViewById<EditText>(R.id.inputKet)
                    val button = addKeyView.findViewById<Button>(R.id.addKey)
                    val dialog_key = AlertDialog.Builder(service).setView(addKeyView)
                        .setOnDismissListener(object : DialogInterface.OnDismissListener {
                            override fun onDismiss(dialog: DialogInterface) {
                                val gJson = Gson().toJson(keyWordList)
                                sharedPreferences!!.edit().putString(KEY_WORD_LIST, gJson)
                                    .apply()
                            }
                        }).create()
                    button.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View) {
                            val input = edit.text.toString()
                            if (!input.isEmpty()) {
                                if (!keyWordList!!.contains(input)) {
                                    val itemView = inflater.inflate(R.layout.keyword_item, null)
                                    val text = itemView.findViewById<TextView>(R.id.keyName)
                                    val rm = itemView.findViewById<TextView>(R.id.remove)
                                    text.text = input
                                    layout.addView(itemView)
                                    keyWordList!!.add(input)
                                    rm.setOnClickListener(object : View.OnClickListener {
                                        override fun onClick(v: View) {
                                            keyWordList!!.remove(text.text.toString())
                                            layout.removeView(itemView)
                                        }
                                    })
                                }
                                edit.setText("")
                            }
                        }
                    })
                    for (e: String? in keyWordList!!) {
                        val itemView = inflater.inflate(R.layout.keyword_item, null)
                        val text = itemView.findViewById<TextView>(R.id.keyName)
                        val rm = itemView.findViewById<TextView>(R.id.remove)
                        text.text = e
                        layout.addView(itemView)
                        rm.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                keyWordList!!.remove(text.text.toString())
                                layout.removeView(itemView)
                            }
                        })
                    }
                    val win = dialog_key.window
                    win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
                    win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                    win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    win.setDimAmount(0f)
                    dialog_key.show()
                    val params = win.attributes
                    params.width = (width / 6) * 5
                    win.attributes = params
                    dialog_adv.dismiss()
                }
                dialog_adv.setOnDismissListener {
                    val gson = Gson()
                    sharedPreferences!!.edit()
                        .putString(ACTIVITY_POSITION, gson.toJson(act_position)).putString(
                            ACTIVITY_WIDGET, gson.toJson(act_widget)
                        ).apply()
                }
                val win = dialog_adv.window
                win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
                win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                win.setDimAmount(0f)
                dialog_adv.show()
                val params = win.attributes
                params.width = width
                win.attributes = params
                dialog_main.dismiss()
                return true
            }
        })
        switch_music_control.setOnLongClickListener {
            val view = inflater.inflate(R.layout.control_music_set, null)
            val seekBar = view.findViewById<SeekBar>(R.id.strength)
            val checkLock = view.findViewById<CheckBox>(R.id.check_lock)
            seekBar.progress = vibration_strength
            checkLock.isChecked = controlMusicOnlyLock
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    vibration_strength = progress
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
            val onCheckedChangeListener: CompoundButton.OnCheckedChangeListener =
                object : CompoundButton.OnCheckedChangeListener {
                    override fun onCheckedChanged(compoundButton: CompoundButton, b: Boolean) {
                        when (compoundButton.id) {
                            R.id.check_lock -> controlMusicOnlyLock = b
                        }
                    }
                }
            checkLock.setOnCheckedChangeListener(onCheckedChangeListener)
            val dialog_vol = AlertDialog.Builder(service).setView(view)
                .setOnDismissListener(object : DialogInterface.OnDismissListener {
                    override fun onDismiss(dialog: DialogInterface) {
                        if (controlMusicOnlyLock) {
                            accessibilityServiceInfo!!.flags =
                                accessibilityServiceInfo!!.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
                        } else if (control_music) {
                            accessibilityServiceInfo!!.flags =
                                accessibilityServiceInfo!!.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                        }
                        service.serviceInfo = accessibilityServiceInfo
                        sharedPreferences.edit()
                            .putInt(VIBRATION_STRENGTH, vibration_strength).putBoolean(
                                CONTROL_MUSIC_ONLY_LOCK, controlMusicOnlyLock
                            ).apply()
                    }
                }).create()
            val win = dialog_vol.window
            win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
            win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
            win.setDimAmount(0f)
            dialog_vol.show()
            val params = win.attributes
            params.width = (width / 6) * 5
            win.attributes = params
            dialog_main.dismiss()
            true
        }
        switch_record_message.setOnLongClickListener(object : OnLongClickListener {
            override fun onLongClick(v: View): Boolean {
                try {
                    val file = File("$savePath/NotificationMessageCache.txt")
                    val view = inflater.inflate(R.layout.view_massage, null)
                    val dialog_message = AlertDialog.Builder(service).setView(view).create()
                    var textView = view.findViewById<EditText>(R.id.editText)
                    val but_choose = view.findViewById<TextView>(R.id.choose)
                    val but_empty = view.findViewById<TextView>(R.id.empty)
                    val but_cancel = view.findViewById<TextView>(R.id.cancel)
                    val but_sure = view.findViewById<TextView>(R.id.sure)
                    but_choose.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View) {
                            val view = inflater.inflate(R.layout.view_select, null)
                            val listView = view.findViewById<ListView>(R.id.listView)
                            val list = packageManager.queryIntentActivities(
                                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                                PackageManager.MATCH_ALL
                            )
                            val listApp = ArrayList<AppInformation>()
                            for (e: ResolveInfo in list) {
                                val info = e.activityInfo.applicationInfo
                                listApp.add(
                                    AppInformation(
                                        info.packageName,
                                        packageManager.getApplicationLabel(info).toString(),
                                        info.loadIcon(packageManager)
                                    )
                                )
                            }
                            val baseAdapter: BaseAdapter = object : BaseAdapter() {
                                override fun getCount(): Int {
                                    return listApp.size
                                }

                                override fun getItem(position: Int): Any {
                                    return position
                                }

                                override fun getItemId(position: Int): Long {
                                    return position.toLong()
                                }

                                override fun getView(
                                    position: Int,
                                    convertView: View?,
                                    parent: ViewGroup
                                ): View {
                                    var localConvertView = convertView
                                    val holder: ViewHolder
                                    if (localConvertView == null) {
                                        localConvertView = inflater.inflate(R.layout.view_pac, null)
                                        holder = ViewHolder(localConvertView)
                                        localConvertView.tag = holder
                                    } else {
                                        holder = localConvertView.tag as ViewHolder
                                    }
                                    val tem = listApp[position]
                                    holder.textView.text = tem.applicationName
                                    holder.imageView.setImageDrawable(tem.applicationIcon)
                                    holder.checkBox.isChecked = pac_msg!!.contains(tem.packageName)
                                    return localConvertView!!
                                }
                            }
                            listView.onItemClickListener =
                                OnItemClickListener { parent, view, position, id ->
                                    val c = (view.tag as ViewHolder).checkBox
                                    val str = listApp[position].packageName
                                    if (c.isChecked) {
                                        pac_msg!!.remove(str)
                                        c.isChecked = false
                                    } else {
                                        pac_msg!!.add(str)
                                        c.isChecked = true
                                    }
                                }
                            listView.adapter = baseAdapter
                            val dialog_pac = AlertDialog.Builder(service).setView(view)
                                .setOnDismissListener {
                                    sharedPreferences.edit().putStringSet(PAC_MSG, pac_msg)
                                        .apply()
                                }.create()
                            val win = dialog_pac.window
                            win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
                            win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                            win.setDimAmount(0f)
                            dialog_pac.show()
                            val params = win.attributes
                            params.width = (width / 6) * 5
                            win.attributes = params
                            dialog_message.dismiss()
                        }

                        inner class AppInformation(
                            var packageName: String,
                            var applicationName: String,
                            var applicationIcon: Drawable
                        ) {
                        }

                        inner class ViewHolder(v: View) {
                            lateinit var textView: TextView
                            var imageView: ImageView
                            var checkBox: CheckBox

                            init {
                                textView = v.findViewById(R.id.name)
                                imageView = v.findViewById(R.id.img)
                                checkBox = v.findViewById(R.id.check)
                            }
                        }
                    })
                    but_empty.setOnClickListener { textView.setText("") }
                    but_cancel.setOnClickListener { dialog_message.dismiss() }
                    but_sure.setOnClickListener {
                        try {
                            val writer = FileWriter(file, false)
                            writer.write(textView.text.toString())
                            writer.close()
                            dialog_message.dismiss()
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                    if (file.exists()) {
                        val builder = StringBuilder()
                        val scanner = Scanner(file)
                        while (scanner.hasNextLine()) {
                            builder.append(scanner.nextLine() + "\n")
                        }
                        scanner.close()
                        textView.setText(builder.toString())
                        textView.setSelection(builder.length)
                    } else {
                        textView.hint = "当前文件内容为空，如果还没有选择要记录其通知的应用，请点击下方‘选择应用’进行勾选。"
                    }
                    val win = dialog_message.window
                    win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
                    win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                    win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    win.setDimAmount(0f)
                    dialog_message.show()
                    val params = win.attributes
                    params.width = width
                    win.attributes = params
                    dialog_main.dismiss()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                return true
            }
        })
        switch_screen_lightness.setOnLongClickListener {
            screenLightness!!.showControlDialog()
            dialog_main.dismiss()
            true
        }
        switchScreenLock.setOnLongClickListener {
            screenLock!!.showSetAreaDialog()
            dialog_main.dismiss()
            true
        }
        switchScreenLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !devicePolicyManager!!.isAdminActive(componentName) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)) {
                val intent = Intent().setComponent(
                    ComponentName(
                        "com.android.settings",
                        "com.android.settings.DeviceAdminSettings"
                    )
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(intent)
                controlLock = false
                dialog_main.dismiss()
            }
        }
        btSetting.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                    "package:$packageName"
                )
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
            dialog_main.dismiss()
        }
        bt_look.setOnClickListener {
            val intent = Intent(service, HelpActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
            dialog_main.dismiss()
        }
        bt_cancel.setOnClickListener { dialog_main.dismiss() }
        bt_sure.setOnClickListener {
            if (switch_skip_advertising.isChecked) {
                accessibilityServiceInfo!!.eventTypes =
                    accessibilityServiceInfo!!.eventTypes or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                skipAdvertising = true
            } else {
                accessibilityServiceInfo!!.eventTypes =
                    accessibilityServiceInfo!!.eventTypes and (AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).inv()
                skipAdvertising = false
            }
            if (switch_music_control.isChecked) {
                if (!controlMusicOnlyLock) {
                    accessibilityServiceInfo!!.flags =
                        accessibilityServiceInfo!!.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                }
                control_music = true
            } else {
                accessibilityServiceInfo!!.flags =
                    accessibilityServiceInfo!!.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
                control_music = false
            }
            if (switch_record_message.isChecked) {
                accessibilityServiceInfo!!.eventTypes =
                    accessibilityServiceInfo!!.eventTypes or AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                record_message = true
            } else {
                accessibilityServiceInfo!!.eventTypes =
                    accessibilityServiceInfo!!.eventTypes and AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED.inv()
                record_message = false
            }
            if (switch_screen_lightness.isChecked) {
                if (!controlLightness) {
                    screenLightness!!.showFloat()
                    controlLightness = true
                }
            } else {
                if (controlLightness) {
                    screenLightness!!.dismiss()
                    controlLightness = false
                }
            }
            if (switchScreenLock.isChecked) {
                if (!controlLock) {
                    screenLock!!.showLockFloat()
                    controlLock = true
                }
            } else {
                if (controlLock) {
                    screenLock!!.dismiss()
                    controlLock = false
                }
            }
            service.serviceInfo = accessibilityServiceInfo
            sharedPreferences.edit().putBoolean(SKIP_ADVERTISING, skipAdvertising)
                .putBoolean(
                    CONTROL_MUSIC, control_music
                ).putBoolean(RECORD_MESSAGE, record_message).putBoolean(
                    CONTROL_LIGHTNESS, controlLightness
                ).putBoolean(CONTROL_LOCK, controlLock).apply()
            dialog_main.dismiss()
        }
        val win = dialog_main.window
        win!!.setBackgroundDrawableResource(R.drawable.dialogbackground)
        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        win.setDimAmount(0f)
        dialog_main.show()
        val params = win.attributes
        params.width = (width / 6) * 5
        win.attributes = params
    }

    companion object {
        private const val TAG = "MyAccessibilityService"
        private const val CONTROL_LIGHTNESS = "control_lightness"
        private const val CONTROL_LOCK = "control_lock"
        private const val SKIP_ADVERTISING = "skip_advertising"
        private const val RECORD_MESSAGE = "record_message"
        private const val CONTROL_MUSIC = "control_music"
        private const val CONTROL_MUSIC_ONLY_LOCK = "control_music_unlock"
        private const val PAC_MSG = "pac_msg"
        private const val VIBRATION_STRENGTH = "vibration_strength"
        private const val ACTIVITY_POSITION = "act_position"
        private const val ACTIVITY_WIDGET = "act_widget"
        private const val PAC_WHITE = "pac_white"
        private const val KEY_WORD_LIST = "keyWordList"
    }
}