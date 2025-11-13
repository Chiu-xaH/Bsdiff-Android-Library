package com.xah.bsdiffs.test

import android.content.pm.PackageManager

object AppVersion {
    private val packageName = MyApplication.context.packageManager.getPackageInfo(MyApplication.context.packageName,0)

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
