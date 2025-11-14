package com.bsdiff.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


// 获取文件bytes
suspend fun getFileSize(
    url: String,
    timeOutTime : Int = 5000
): Long? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connectTimeout = timeOutTime
        connection.readTimeout = timeOutTime
        connection.connect()
        val length = connection.contentLengthLong
        connection.disconnect()
        length
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


// 下载文件，返回进度 0..100
fun downloadFile(
    context: Context,
    url: String,
    fileName: String,
    delayTimesLong: Long = 1000L,
    requestBuilder: (DownloadManager.Request) -> DownloadManager.Request = { it },
    customDownloadId: Long? = null
): Flow<DownloadProgress> = flow {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    // 下载到Download
    val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    if (!destDir.exists()) {
        destDir.mkdirs()
    }
    // 移除已有文件
    val destFile = File(destDir, fileName)
    if (destFile.exists()) {
        destFile.delete()
    }

    var request = DownloadManager.Request(url.toUri())
        .setTitle(fileName)
        .setDescription("Downloading $fileName")
        .setDestinationUri(Uri.fromFile(destFile))
        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

    // 开发者自定义Request
    request = requestBuilder(request)
    // 自定义ID或默认分配 开发者持有ID可自定义更多操作
    val downloadId = customDownloadId ?: downloadManager.enqueue(request)

    val query = DownloadManager.Query().setFilterById(downloadId)
    var downloading = true
    // 轮询进度
    while (downloading) {
        val cursor = downloadManager.query(query)
        if (cursor != null && cursor.moveToFirst()) {
            val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

            if (bytesTotal > 0) {
                val progress = (bytesDownloaded * 100 / bytesTotal).toInt()
                emit(DownloadProgress(downloadId, progress))
            }

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                emit(DownloadProgress(downloadId, 100))
                downloading = false
            } else if (status == DownloadManager.STATUS_FAILED) {
                downloading = false
                throw IOException("Download failed for $url")
            }
        }
        cursor?.close()
        delay(delayTimesLong)
    }
    // 下载完成
    emit(DownloadProgress(downloadId, 100))
}.flowOn(Dispatchers.IO)
