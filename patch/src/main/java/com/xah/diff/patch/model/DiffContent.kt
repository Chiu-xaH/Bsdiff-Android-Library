package com.xah.diff.patch.model

import java.io.File

data class DiffContent(
    val targetFileMd5 : String?,
    val diffFile: File
)




