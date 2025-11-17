package com.xah.shared.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object InstallUtils {
    // 安装Apk
    fun installApk(
        apkFile : File,
        context: Context,
        authority : String = ".provider",
        onNotFound : (() -> Unit)? = null,
    ) {
        val packageName = context.packageName
        if (!apkFile.exists()) {
            onNotFound?.let { it() }
            return
        }
        val uri = FileProvider.getUriForFile(context, packageName + authority, apkFile)
        installApk(uri,context)
    }

    fun installApk(
        uri : Uri,
        context : Context
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }
}