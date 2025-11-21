package com.xah.shared.result

enum class DiffErrorCode(val code: Int) {
    SOURCE_APK_NOT_FOUND(1000),// 源Apk在工作目录内未找到，可能是复制Apk时出现问题
    DIFF_FILE_NOT_FOUND(1001), // 补丁包未找到，可能是被删除了
    MERGE_FAILED(1002), // Native层合并失败，原因将以代号的形式置于message中
    MD5_MISMATCH(1003), // MD5校验不通过
}