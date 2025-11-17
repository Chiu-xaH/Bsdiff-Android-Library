package com.xah.patch.meta.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatchMeta(
    val md5: String,
    @SerialName("version_code")
    val versionCode: Long?,
    @SerialName("version_name")
    val versionName: String?
)