package com.xah.bsdiffs.diff.model

import java.io.File

data class DiffContent(
    val targetFileMd5 : String,
    val diffFile: File
)