package com.jinyx.facedemo.expand

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import java.io.File
import java.io.FileInputStream
import java.util.*


@SuppressLint("PrivateApi")
fun Activity.initStatusBar() { // Android 7.0 以上去除灰色遮罩层
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {   // system_bar_background_semi_transparent
            val decorViewClazz = Class.forName("com.android.internal.policy.DecorView")
            val field = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                decorViewClazz.getDeclaredField("mSemiTransparentBarColor")
            } else {
                decorViewClazz.getDeclaredField("mSemiTransparentStatusBarColor")
            }
            field.isAccessible = true
            field.setInt(window.decorView, Color.TRANSPARENT)  //改为透明
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun Activity.setStatusBarLightMode(isDark: Boolean): Boolean {
    return when {
        isMiUIV6OrAbove() -> {
            setMIUIStatusBarLightMode(isDark)
        }
        isFlymeV4OrAbove() -> {
            setFlymeStatusBarLightMode(isDark)
        }
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isDark) {    //设置状态栏黑色字体
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {    // 状态栏白色字体
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
                true
            } else {
                false
            }
        }
    }
}

@SuppressLint("PrivateApi")
private fun Activity.setMIUIStatusBarLightMode(isDark: Boolean): Boolean {
    var result = false
    if (window != null) {
        val clazz = window::class.java
        try {
            val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
            val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
            val darkModeFlag = field.getInt(layoutParams)
            val extraFlagField = clazz.getMethod(
                "setExtraFlags",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            if (isDark) {
                extraFlagField.invoke(window, darkModeFlag, darkModeFlag)//状态栏透明且黑色字体
            } else {
                extraFlagField.invoke(window, 0, darkModeFlag)//清除黑色字体
            }
            result = true
        } catch (e: Exception) {
        }
    }
    return result
}

private fun Activity.setFlymeStatusBarLightMode(isDark: Boolean): Boolean {
    var result = false
    if (window?.attributes != null) {
        try {
            val lp = window.attributes!!
            val darkFlag = WindowManager.LayoutParams::class.java
                .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
            val meiZuFlags = WindowManager.LayoutParams::class.java
                .getDeclaredField("meizuFlags")
            darkFlag.isAccessible = true
            meiZuFlags.isAccessible = true
            val bit = darkFlag.getInt(null)
            var value = meiZuFlags.getInt(lp)
            value = if (isDark) {
                value or bit
            } else {
                value and bit.inv()
            }
            meiZuFlags.setInt(lp, value)
            window.attributes = lp
            result = true
        } catch (e: Exception) {
        }
    }
    return result
}

private fun isMiUIV6OrAbove(): Boolean {
    return try {
        val properties = Properties()
        properties.load(FileInputStream(File(Environment.getRootDirectory(), "build.prop")))
        val uiCode = properties.getProperty("ro.miui.ui.version.code", null)
        val code = uiCode?.toIntOrNull() ?: 0
        code >= 4
    } catch (e: Exception) {
        false
    }
}

private fun isFlymeV4OrAbove(): Boolean {
    val displayId = Build.DISPLAY
    if (!TextUtils.isEmpty(displayId) && displayId.contains("Flyme")) {
        val displayIdArray =
            displayId.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (temp in displayIdArray) {
            //版本号4以上，形如4.x.
            if (temp.matches("^[4-9]\\.(\\d+\\.)+\\S*".toRegex())) {
                return true
            }
        }
    }
    return false
}

fun Activity.hideNavigationBar() {
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
}

fun Activity.transparentNavigationBar() {
    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
}