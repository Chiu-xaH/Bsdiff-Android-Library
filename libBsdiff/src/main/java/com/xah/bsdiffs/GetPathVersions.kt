package com.xah.bsdiffs

import com.xah.bsdiffs.model.Patch

// 传入文件名，返回Patch
fun parsePatchFile(fileName : String) : Patch? {
    val e = ".patch"
    return if (fileName.endsWith(e)) {
        // 以.patch结尾，代表补丁
        val str = fileName.substringBefore(e)
        // 取旧版本号与新版本号
        val versions = str.split("_to_")
        if(versions.size != 2) {
            return null
        }
        Patch(versions[0],versions[1])
    } else {
        null
    }
}
// 传入Patch，返回文件名
fun parsePatch(patch : Patch) : String = patch.oldVersion + "_to_" + patch.newVersion + ".patch"