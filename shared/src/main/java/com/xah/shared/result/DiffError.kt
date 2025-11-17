package com.xah.shared.result

data class DiffError(
    val code: DiffErrorCode,
    val message: String
)