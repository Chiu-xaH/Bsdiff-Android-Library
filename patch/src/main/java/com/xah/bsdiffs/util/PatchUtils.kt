package com.xah.bsdiffs.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

// 获取包名
fun getPackageName(context: Context) : String = context.packageName

// 安装Apk
fun installApk(
    context: Context,
    apkFile : File,
    authority : String = ".provider",
    onNotFound : (() -> Unit)? = null,
) {
    val packageName = getPackageName(context)
    if (!apkFile.exists()) {
        onNotFound?.let { it() }
        return
    }
    val uri = FileProvider.getUriForFile(context, packageName + authority, apkFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}