package com.xah.bsdiff.networktest.logic.util

import android.content.pm.PackageManager
import com.xah.bsdiff.networktest.application.MyApplication

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

    val sdkInt = android.os.Build.VERSION.SDK_INT

    val needPermission = sdkInt < 29
}