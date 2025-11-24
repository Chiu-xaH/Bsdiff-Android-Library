package com.xah.diff.shared.result

data class DiffError(
    val code: DiffErrorCode,
    val message: String
)