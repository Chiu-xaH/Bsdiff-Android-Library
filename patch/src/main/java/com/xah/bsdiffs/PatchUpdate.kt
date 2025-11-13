package com.xah.bsdiffs

import android.content.Context
import android.util.Log
import com.xah.bsdiffs.model.Patch
import com.xah.bsdiffs.model.PatchContent
import com.xah.bsdiffs.util.getPackageName
import com.xah.bsdiffs.util.installApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipFile

class PatchUpdate {
    private val bsdiff = BsdiffJni()

    companion object {
        private const val CACHE_DIR = "patch_temp"
    }

    // 解析Patch文件
    private fun parsePatchFile(patchFile: File, context : Context): PatchContent {
        require(patchFile.exists()) { "File Not Found: ${patchFile.path}" }

        ZipFile(patchFile).use { zip ->
            val metaEntry = zip.getEntry("meta.json")
                ?: error("meta.json is not existed")
            val metaText = zip.getInputStream(metaEntry).bufferedReader().use { it.readText() }

            val meta = Json.Default.decodeFromString<Patch>(metaText)

            val diffEntry = zip.getEntry("diff.bin")
                ?: error("diff.bin is not existed")

            val diffOutFile =
                File(getPatchCacheDir(context), "diff_${System.currentTimeMillis()}.bin")

            zip.getInputStream(diffEntry).use { input ->
                diffOutFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return PatchContent(meta, diffOutFile)
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
    private fun copySourceApk(context: Context): File? {
        val sourceApk = File(context.packageCodePath)
        val destDir = getPatchCacheDir(context)
        // 使用固定名称，保证只有一个 防止重复复制
        val destFile = File(destDir, "source.apk")

        return try {
            if (!destDir.exists()) destDir.mkdirs()

            // 如果文件已经存在，就不再重复复制
            if (!destFile.exists()) {
                FileInputStream(sourceApk).channel.use { input ->
                    FileOutputStream(destFile).channel.use { output ->
                        input.transferTo(0, input.size(), output)
                    }
                }
            }

            destFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // 计算MD5
    private fun getMd5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    // 检查是否适用当前版本并且Patch完整 完整则返回双方的File
    private fun checkPatch(patchContent : PatchContent, context : Context) : Pair<File, File>? {
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
    private fun checkTarget(targetFile: File, patchContent: PatchContent): Boolean {
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