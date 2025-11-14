package com.bsdiff.core

class BsdiffJni {
    companion object {
        init {
            System.loadLibrary("bsdiff")
        }
    }
    // 0为成功 具体见源码
    external fun patch(oldFilePath: String, newFilePath: String, patchFilePath: String) : Int
    external fun merge(oldFilePath: String, patchFilePath: String, newFilePath: String) : Int
}