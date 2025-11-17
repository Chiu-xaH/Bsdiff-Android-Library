package com.xah.shared.download.model

import android.net.Uri
import java.io.File

sealed class DownloadResult {
    data class Downloaded(val file : File) : DownloadResult()
    data class Progress(val downloadId: Long, val progress: Int) : DownloadResult()
    data class Success(val downloadId: Long, val file: File, val uri: Uri) : DownloadResult()
    data class Failed(val downloadId: Long, val reason: String?) : DownloadResult()
    data class Prepare(val fileSize : Long? = null) : DownloadResult()
}