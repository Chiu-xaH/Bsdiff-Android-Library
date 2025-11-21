package com.xah.shared.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.xah.shared.result.DiffResult
import com.xah.shared.util.InstallUtils.installApk
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

// 合并完成后的默认操作
fun mergedDefaultFunction(
    result : DiffResult,
    context: Context,
    authority : String = ".provider",
) {
    when(result) {
        is DiffResult.Success -> {
            val targetFile = result.file
            // 安装
            installApk (targetFile,context,authority) {
                Toast.makeText(context,"Not found target apk to install", Toast.LENGTH_SHORT).show()
            }
        }
        is DiffResult.Error -> {
            // 错误
            val error = result.error
            Log.e("DiffUpdate","code: " + error.code + "\nmessage: " + error.message)
            Toast.makeText(context,error.message, Toast.LENGTH_SHORT).show()
        }
    }
}


fun uriToFile(context: Context, uri: Uri): File? {
    val contentResolver = context.contentResolver
    val fileName = queryName(contentResolver, uri)
    val tempFile = File(context.cacheDir, fileName ?: "temp_patch_${System.currentTimeMillis()}.patch")

    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun queryName(resolver: ContentResolver, uri: Uri): String? {
    val returnCursor = resolver.query(uri, null, null, null, null)
    returnCursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && it.moveToFirst()) {
            return it.getString(nameIndex)
        }
    }
    return null
}
