package com.xah.bsdiffs.diff

import android.content.Context
import com.xah.bsdiffs.diff.model.DiffContent
import com.xah.bsdiffs.util.BsdiffJni
import com.xah.bsdiffs.util.copySourceApkTo
import com.xah.bsdiffs.util.getMd5
import com.xah.bsdiffs.util.getPackageName
import com.xah.bsdiffs.util.installApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DiffUpdate {
    private val bsdiff = BsdiffJni()

    companion object {
        private const val CACHE_DIR = "diff_temp"
    }

    // 建立并返回工作目录
    private fun getPatchCacheDir(context: Context): File {
        val baseDir = context.cacheDir
        val dir = File(baseDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // 将源Apk复制到工作目录
    private fun copySourceApk(context: Context): File? = copySourceApkTo(context,getPatchCacheDir(context))
    // 校验合成包
    private fun checkTarget(targetFile: File, diffContent: DiffContent): Boolean {
        if (!targetFile.exists()) return false
        val md5 = getMd5(targetFile)
        return md5 == diffContent.targetFileMd5
    }

    // 清理工作目录
    private fun clean(context: Context, exclude: File? = null) {
        val dir = getPatchCacheDir(context)
        dir.listFiles()?.forEach {
            if (it != exclude) it.delete()
        }
    }

    fun clean(context: Context) = clean(context,null)

    // 合并补丁包
    suspend fun merge (
        diffContent: DiffContent,
        context : Context,
    ) : File? = withContext(Dispatchers.IO) {
        // 复制本体Apk到工作目录
        val sourceApk = copySourceApk(context) ?: error("无法获取到源Apk")
        // 合并文件的叫target.apk 若放在工作目录，则需要安装完成后清理缓存
        val targetFile =
            File(getPatchCacheDir(context), "${getPackageName(context)}_patch_target.apk")
        // 合并
        bsdiff.merge(sourceApk.absolutePath, diffContent.diffFile.absolutePath, targetFile.absolutePath)
        // 进行校验
        val checkResult = checkTarget(targetFile, diffContent)
        if (!checkResult) {
            clean(context)
            throw Exception("MD5校验失败 期望${diffContent.targetFileMd5} 实际${getMd5(targetFile)}")
            return@withContext null
        }
        // 清理工作目录 新合成文件除外
        clean(context, targetFile)
        return@withContext targetFile
    }

    // 合并补丁包回调版本
    suspend fun mergeCallback (
        diffContent: DiffContent,
        context : Context,
        onResult : (File?) -> Unit = { file ->
            file?.let {
                // 安装
                installApk(context, it) {
                    error("Not Found Target Apk")
                }
                // 删除
                clean(context, it)
            }
        },
    )  = withContext(Dispatchers.IO) {
        val targetFile = merge(diffContent, context)
        // 允许开发者定制成功后的操作 调用安装
        onResult(targetFile)
    }
}