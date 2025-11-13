package com.xah.bsdiffs.util

class BsdiffJni {
    companion object {
        init {
            System.loadLibrary("bsdiff")
        }
    }

    external fun patch(oldFilePath: String, newFilePath: String, patchFilePath: String) : Int
    external fun merge(oldFilePath: String, patchFilePath: String, newFilePath: String) : Int
}