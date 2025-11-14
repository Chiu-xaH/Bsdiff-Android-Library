package com.xah.bsdiffs.result

import java.io.File

sealed class DiffResult {
    data class Success(val file: File) : DiffResult()
    data class Error(val error: DiffError) : DiffResult()
}