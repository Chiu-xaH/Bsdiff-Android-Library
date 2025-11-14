package com.xah.bsdiffs.result

data class DiffError(
    val code: DiffErrorCode,
    val message: String
)