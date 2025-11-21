package com.xah.bsdiff.networktest.logic.util

import com.xah.bsdiff.networktest.ui.util.CONNECTION_ERROR_CODE
import com.xah.bsdiff.networktest.ui.util.OPERATION_FAST_ERROR_CODE
import com.xah.bsdiff.networktest.ui.util.PARSE_ERROR_CODE
import com.xah.bsdiff.networktest.ui.util.StateHolder
import com.xah.bsdiff.networktest.ui.util.TIMEOUT_ERROR_CODE
import com.xah.bsdiff.networktest.ui.util.UNKNOWN_ERROR_CODE
import okhttp3.Headers
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.awaitResponse

// 通用方法用于解析响应（根据需要进行调整）
@Suppress("UNCHECKED_CAST")
private fun <T> parseResponse(responseBody: String?): T? {
    return responseBody as? T
}

suspend fun <T> launchRequestState(
    holder: StateHolder<T>,
    request: suspend () -> Call<ResponseBody>,
    transformSuccess: suspend (Headers, String) -> T,
    transformRedirect: ((Headers) -> T)? = null
) = try {
    holder.setLoading()
    val response = request().awaitResponse()
    val headers = response.headers()
    val bodyString = response.body()?.string().orEmpty()

    if (response.isSuccessful) {
        // 成功
        val result = try {
            transformSuccess(headers, bodyString)
        } catch (e: Exception) {
            holder.emitError(e, PARSE_ERROR_CODE)
            return
        }
        holder.emitData(result)
    }
    else if(response.code() == StatusCode.REDIRECT.code){
//             重定向 特殊处理
        val result = try {
            transformRedirect!!(headers)
        } catch (e: Exception) {
            holder.emitError(e, PARSE_ERROR_CODE)
            return
        }
        holder.emitData(result)
    }
    else {
        // 承接错误解析 可选
        holder.emitError(HttpException(response), response.code())
    }
} catch (e: Exception) {
    e.printStackTrace()
    holder.emitError(e,null)
}
// 空响应
suspend fun <T> launchRequestState(
    holder: StateHolder<T>,
    request: suspend () -> Call<Void>,
    transformSuccess: suspend (Headers) -> T,
    transformRedirect: ((Headers) -> T)? = null
) = try {
    holder.setLoading()
    val response = request().awaitResponse()
    val headers = response.headers()

    if (response.isSuccessful) {
        // 成功
        val result = try {
            transformSuccess(headers)
        } catch (e: Exception) {
            holder.emitError(e, PARSE_ERROR_CODE)
            return
        }
        holder.emitData(result)
    }
    else if(response.code() == StatusCode.REDIRECT.code){
//             重定向 特殊处理
        val result = try {
            transformRedirect!!(headers)
        } catch (e: Exception) {
            holder.emitError(e, PARSE_ERROR_CODE)
            return
        }
        holder.emitData(result)
    }
    else {
        // 承接错误解析 可选
        holder.emitError(HttpException(response), response.code())
    }
} catch (e: Exception) {
    holder.emitError(e,null)
}
// 无需关心内容 只关心请求结果
suspend fun launchRequestNone(
    request: suspend () -> Call<ResponseBody>,
) : Int = try {
    val response = request().awaitResponse()
    response.code()
} catch (e : Exception) {
    e.printStackTrace()
    val eMsg = e.message
    if(eMsg?.contains("10000ms") == true) {
        TIMEOUT_ERROR_CODE
    } else if(eMsg?.contains("Unable to resolve host",ignoreCase = true) == true || eMsg?.contains("Failed to connect to",ignoreCase = true) == true ||  eMsg?.contains("Connection reset",ignoreCase = true) == true) {
        CONNECTION_ERROR_CODE
    } else if(eMsg?.contains("The coroutine scope") == true) {
        OPERATION_FAST_ERROR_CODE
    } else {
        UNKNOWN_ERROR_CODE
    }
}


