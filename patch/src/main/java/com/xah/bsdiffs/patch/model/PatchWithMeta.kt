package com.xah.bsdiffs.patch.model

import kotlinx.serialization.Serializable

@Serializable
data class PatchWithMeta(
    val source : PatchMeta,
    val target : PatchMeta
)



