package com.xah.shared.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

// 计算MD5
fun getMd5(file: File): String {
    val md = MessageDigest.getInstance("MD5")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}

// 将源Apk复制到工作目录
fun copySourceApkTo(context: Context,destDir : File): File? {
    val sourceApk = File(context.packageCodePath)
    // 使用固定名称，保证只有一个 防止重复复制
    val destFile = File(destDir, "source.apk")

    return try {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        // 如果文件已经存在，就不再重复复制
        if (!destFile.exists()) {
            FileInputStream(sourceApk).channel.use { input ->
                FileOutputStream(destFile).channel.use { output ->
                    input.transferTo(0, input.size(), output)
                }
            }
        }

        destFile
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}