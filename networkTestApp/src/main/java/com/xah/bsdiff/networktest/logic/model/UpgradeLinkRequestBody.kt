package com.xah.bsdiff.networktest.logic.model

import android.util.Log
import com.google.gson.Gson
import com.xah.bsdiff.networktest.logic.network.repo.UpgradeLinkRepository
import com.xah.bsdiff.networktest.logic.util.AppVersion
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID


private fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("GMT+8")
    return sdf.format(Date())
}
/*
X-Timestamp	是	string	2025-02-17T10:34:55+08:00	请求时间 RFC3339格式
X-Nonce	是	string	fc812cc0b9b51e8c	唯一随机字符串(至少16位)
X-AccessKey	是	string	mui2W50H1j-OC4xD6PgQag	密钥 AccessKey
 */

data class UpgradeLinkRequestBody(
    val versionCode : Int = AppVersion.getVersionCode(),
    val apkKey : String = UpgradeLinkRepository.APK_KEY,
    val patchAlgo : Int = PatchAlgo.DIFF.code
)

private fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

data class UpgradeLinkRequestBean(
    val body : UpgradeLinkRequestBody,
    val timestamp: String = getCurrentTimestamp(),
    val nonce : String = UUID.randomUUID().toString().replace("-", ""),
) {
    fun signature() : String {
        val bodyJson = Gson().toJson(body)
        val signStr = "body=${bodyJson}&nonce=${nonce}&secretKey=${UpgradeLinkRepository.SECRET_KEY}&timestamp=${timestamp}&url=/${UpgradeLinkRepository.API}"
        return md5(signStr)
    }
    val signature : String = signature()
}

fun getUpgradeLinkRequestBean() = UpgradeLinkRequestBean(body = UpgradeLinkRequestBody())