package com.xah.bsdiffs.patch

import android.content.Context
import android.util.Log
import com.xah.bsdiffs.util.BsdiffJni
import com.xah.bsdiffs.patch.model.PatchWithMeta
import com.xah.bsdiffs.patch.model.PatchWithMetaContent
import com.xah.bsdiffs.util.copySourceApkTo
import com.xah.bsdiffs.util.getMd5
import com.xah.bsdiffs.util.getPackageName
import com.xah.bsdiffs.util.installApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipFile

class DiffWithMetaUpdate {
    private val bsdiff = BsdiffJni()

    companion object {
        private const val CACHE_DIR = "patch_temp"
    }

    // 解析Patch文件
    private fun parsePatchFile(patchFile: File, context : Context): PatchWithMetaContent {
        require(patchFile.exists()) { "File Not Found: ${patchFile.path}" }

        ZipFile(patchFile).use { zip ->
            val metaEntry = zip.getEntry("meta.json")
                ?: error("meta.json is not existed")
            val metaText = zip.getInputStream(metaEntry).bufferedReader().use { it.readText() }

            val meta = Json.Default.decodeFromString<PatchWithMeta>(metaText)

            val diffEntry = zip.getEntry("diff.bin")
                ?: error("diff.bin is not existed")

            val diffOutFile =
                File(getPatchCacheDir(context), "diff_${System.currentTimeMillis()}.bin")

            zip.getInputStream(diffEntry).use { input ->
                diffOutFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return PatchWithMetaContent(meta, diffOutFile)
        }
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

    // 检查是否适用当前版本并且Patch完整 完整则返回双方的File
    private fun checkPatch(patchContent : PatchWithMetaContent, context : Context) : Pair<File, File>? {
        // 复制源Apk
        val sourceApk = copySourceApk(context) ?: error("无法获取到源Apk")
        val sourceMd5 = getMd5(sourceApk)
        val patchMd5 = patchContent.meta.source.md5

        if (sourceMd5 != patchMd5) {
            Log.e("PatchUpdate", "MD5 不匹配，补丁不可用")
            return null
        }

        if (!patchContent.diffFile.exists()) {
            Log.e("PatchUpdate", "diff 文件不存在")
            return null
        }

        return Pair(sourceApk, patchContent.diffFile)
    }

    // 校验合成包
    private fun checkTarget(targetFile: File, patchContent: PatchWithMetaContent): Boolean {
        if (!targetFile.exists()) return false
        val md5 = getMd5(targetFile)
        return md5 == patchContent.meta.target.md5
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
        patchFile: File,
        context : Context,
    ) : File? = withContext(Dispatchers.IO) {
        // 解析Patch文件
        val patchContent = parsePatchFile(patchFile, context)
        // 检查Patch文件
        val canUse = checkPatch(patchContent, context)
        if (canUse == null) {
            clean(context)
            return@withContext null
        }
        // 合并文件的叫target.apk 若放在工作目录，则需要安装完成后清理缓存
        val targetFile =
            File(getPatchCacheDir(context), "${getPackageName(context)}_patch_target.apk")
        // 合并
        bsdiff.merge(canUse.first.absolutePath, canUse.second.absolutePath, targetFile.absolutePath)
        // 进行校验
        val checkResult = checkTarget(targetFile, patchContent)
        if (!checkResult) {
            clean(context)
            throw Exception("MD5校验失败 期望${patchContent.meta.source.md5} 实际${getMd5(targetFile)}")
            return@withContext null
        }
        // 清理工作目录 新合成文件除外
        clean(context, targetFile)
        return@withContext targetFile
    }

    // 合并补丁包回调版本
    suspend fun mergeCallback (
        patchFile: File,
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
        val targetFile = merge(patchFile, context)
        // 允许开发者定制成功后的操作 调用安装
        onResult(targetFile)
    }
}