package com.jinyx.facedemo.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

object ToastUtil {

    private var mToast: Toast? = null

    fun showShortToast(context: Context?, @StringRes resId: Int) {
        showToast(context, context?.getString(resId), Toast.LENGTH_SHORT)
    }

    fun showShortToast(context: Context?, msg: String?) {
        showToast(context, msg, Toast.LENGTH_SHORT)
    }

    fun showLongToast(context: Context?, @StringRes resId: Int) {
        showToast(context, context?.getString(resId), Toast.LENGTH_LONG)
    }

    fun showLongToast(context: Context?, msg: String?) {
        showToast(context, msg, Toast.LENGTH_LONG)
    }

    @SuppressLint("ShowToast")
    private fun showToast(context: Context?, msg: String?, duration: Int) {
        if (msg.isNullOrEmpty() || context == null || (context is Activity && context.isDestroyed)) return
        mToast?.cancel()
        mToast = Toast.makeText(context, msg, duration)
        mToast!!.duration = duration
        mToast!!.setText(msg)
        mToast!!.show()
    }

}