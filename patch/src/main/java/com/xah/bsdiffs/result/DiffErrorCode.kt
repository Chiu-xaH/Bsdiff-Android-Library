package com.xah.bsdiffs.result

enum class DiffErrorCode(val code: Int) {
    SOURCE_APK_NOT_FOUND(1000),
    DIFF_FILE_NOT_FOUND(1001),
    MERGE_FAILED(1002),
    MD5_MISMATCH(1003),
    UNKNOWN(999)
}