package com.xah.bsdiff.networktest.logic.model

data class UpgradeLinkResponseBody(
    val data : UpgradeLinkBean
)

data class UpgradeLinkBean(
    val versionName : String,
    val versionCode : Int,
    val urlPath : String,
    val urlFileSize : Long,
    val urlFileMd5 : String,
    val patchAlgo : Int,
    val patchUrlPath : String,
    val patchUrlFileSize : Long,
    val patchUrlFileMd5 : String,
    val upgradeType : Int,
    val promptUpgradeContent : String
)
