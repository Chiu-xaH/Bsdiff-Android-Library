package com.xah.bsdiff.networktest.logic.network.api

import com.xah.bsdiff.networktest.logic.network.repo.UpgradeLinkRepository
import com.xah.bsdiff.networktest.logic.util.UpgradeLinkRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface UpgradeLinkService {
    @POST(UpgradeLinkRepository.API)
    @Headers("Content-Type: application/json","X-AccessKey: ${UpgradeLinkRepository.ACCESS_KEY}")
    fun getUpgrade(
        @Header("X-Timestamp") timestamp: String,
        @Header("X-Nonce") nonce : String,
        @Header("X-Signature") signature: String,
        @Body body : UpgradeLinkRequestBody
    ) : Call<ResponseBody>
}