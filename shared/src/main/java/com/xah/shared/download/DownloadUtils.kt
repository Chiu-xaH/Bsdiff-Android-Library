package com.xah.shared.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import com.xah.shared.download.model.DownloadResult
import com.xah.shared.util.getMd5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object DownloadUtils {
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

    // 初始化 获取下载文件的大小、判断是否下载过(可选校验MD5)
    fun initDownloadFileStatus(
        fileName: String,
        destDir : File? = null,
        fileMd5 : String?,
        fileSize : Long?,
    ) : DownloadResult {
        val finalDestDir = destDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destFile = File(finalDestDir, fileName)

        return if(checkFile(destFile,fileMd5)) {
            DownloadResult.Downloaded(destFile)
        } else {
            DownloadResult.Prepare(fileSize)
        }
    }

    private fun checkFile(
        file : File,
        fileMd5 : String?,
    ) : Boolean {
        // 文件已经存在
        if (file.exists()) {
            // 不需要校验MD5
            if (fileMd5 == null) {
                return true
            }
            // 需要校验MD5
            if (fileMd5 == getMd5(file)) {
                return true
            }
        }
        // MD5不一致或文件不存在
        return false
    }

    /**
     * @param context 上下文
     * @param url 下载链接
     * @param fileName 文件名，记得带扩展名
     * @param fileMd5 期望MD5，用于检验是否下载过或者下载完成后是否符合期望
     * @param destDir 下载目录，默认为null代表下载到公有Download目录(需存储权限)
     * @param delayTimesLong 下载进度更新间隔，单位为毫秒
     * @param requestBuilder 自定义下载器构建
     * @param customDownloadId 自定义下载ID，默认为null代表由系统分配，下载过程会返回id
     */
    fun downloadFile(
        context: Context,
        url: String,
        fileName: String,
        fileMd5 : String? = null,
        destDir: File? = null,
        delayTimesLong: Long = 1000L,
        requestBuilder: (DownloadManager.Request) -> DownloadManager.Request = { it },
        customDownloadId: Long? = null
    ): Flow<DownloadResult> = flow {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // 如果开发者没有传目录，默认使用公共 Download 目录
        val finalDestDir = destDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destFile = File(finalDestDir, fileName)
        // 判断是否存在文件
        if (checkFile(destFile,fileMd5)) {
            emit(DownloadResult.Downloaded(destFile))
            return@flow
        }

        var request = DownloadManager.Request(url.toUri())
            .setTitle(fileName)
            .setDescription("Download $fileName")
            .setDestinationUri(Uri.fromFile(destFile))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .addRequestHeader("Range", "bytes=0-")

        request = requestBuilder(request)

        val downloadId = customDownloadId ?: downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)

        var downloading = true
        while (downloading) {
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                if (bytesTotal > 0) {
                    val progress = (bytesDownloaded * 100 / bytesTotal).toInt()
                    emit(DownloadResult.Progress(downloadId, progress))
                }

                when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        downloading = false
                        emit(
                            DownloadResult.Success(
                                downloadId = downloadId,
                                file = destFile,
                                uri = Uri.fromFile(destFile),
                                checked = checkFile(destFile,fileMd5)
                            )
                        )
                    }
                    DownloadManager.STATUS_FAILED -> {
                        downloading = false
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)).toString()
                        Log.e("DownloadManager", "Download Failed,DownloadManager error code with: $reason")
                        emit(DownloadResult.Failed(downloadId, reason))
                    }
                }
            }
            cursor?.close()
            delay(delayTimesLong)
        }
    }.flowOn(Dispatchers.IO)
}


