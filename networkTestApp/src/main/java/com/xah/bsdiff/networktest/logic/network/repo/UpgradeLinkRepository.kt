package com.xah.bsdiff.networktest.logic.network.repo

import com.google.gson.Gson
import com.xah.bsdiff.networktest.logic.network.api.UpgradeLinkService
import com.xah.bsdiff.networktest.logic.network.client.UpgradeLinkServiceCreator
import com.xah.bsdiff.networktest.logic.util.UpgradeLinkBean
import com.xah.bsdiff.networktest.logic.util.UpgradeLinkResponseBody
import com.xah.bsdiff.networktest.logic.util.getUpgradeLinkRequestBean
import com.xah.bsdiff.networktest.logic.util.launchRequestState
import com.xah.bsdiff.networktest.ui.util.StateHolder

object UpgradeLinkRepository {
    const val API = "v1/apk/upgrade"
    const val HOST = "https://api.upgrade.toolsetlink.com/"
    const val ACCESS_KEY = "vTQjYxXjGwjLFVfGERO29Q"
    const val SECRET_KEY = "t71uf-eUIpeVU5QgM58gKFQaut1pxdpD9Mn2K-RUF9s"
    const val APK_KEY = "dm_LyKkufkJ3YYf0tbC81A"

    private val upgradeLink = UpgradeLinkServiceCreator.create(UpgradeLinkService::class.java)

    suspend fun getUpgrade(holder: StateHolder<UpgradeLinkBean>) = launchRequestState(
        holder = holder,
        request = {
            val request = getUpgradeLinkRequestBean()
            upgradeLink.getUpgrade(
                timestamp = request.timestamp,
                nonce = request.nonce,
                signature = request.getSignature(),
                body = request.body
            )
        },
        transformSuccess = { _,body -> parseUpgrade(body) }
    )

    private fun parseUpgrade(body : String) : UpgradeLinkBean =
        try {
            Gson().fromJson(body, UpgradeLinkResponseBody::class.java).data
        } catch (e : Exception) {
            throw e
        }
}