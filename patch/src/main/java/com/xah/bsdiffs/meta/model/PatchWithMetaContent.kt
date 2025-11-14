package com.xah.bsdiffs.meta.model

import java.io.File

data class PatchWithMetaContent(
    val meta: PatchWithMeta,
    val diffFile: File
)