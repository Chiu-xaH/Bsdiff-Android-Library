package com.xah.bsdiffs.patch.model

import java.io.File

data class PatchWithMetaContent(
    val meta: PatchWithMeta,
    val diffFile: File
)