package com.xah.bsdiffs.test.util

import android.content.pm.PackageManager
import com.xah.bsdiffs.test.application.MyApplication

object AppVersion {
    private val packageName = MyApplication.Companion.context.packageManager.getPackageInfo(
        MyApplication.Companion.context.packageName,0)

    fun getVersionCode() : Int {
        var versionCode = 0
        try {
            versionCode = packageName.versionCode
        } catch ( e : PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionCode
    }


    fun getVersionName() : String {
        var versionName = ""
        try {
            versionName = packageName.versionName.toString()
        } catch ( e : PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionName
    }
}