package com.jinyx.facedemo

import androidx.multidex.MultiDexApplication

class AppPhone : MultiDexApplication() {

    companion object {
        lateinit var instance: AppPhone
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}