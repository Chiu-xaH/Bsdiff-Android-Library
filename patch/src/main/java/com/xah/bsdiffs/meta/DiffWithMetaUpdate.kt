package com.xah.bsdiffs.meta

import android.content.Context
import android.widget.Toast
import com.bsdiff.core.BsdiffJni
import com.xah.bsdiffs.meta.model.PatchWithMeta
import com.xah.bsdiffs.meta.model.PatchWithMetaContent
import com.xah.bsdiffs.result.DiffError
import com.xah.bsdiffs.result.DiffErrorCode
import com.xah.bsdiffs.result.DiffResult
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
        // 必须传入存在的文件
        require(patchFile.exists()) { "File not found: ${patchFile.path}" }

        ZipFile(patchFile).use { zip ->
            val metaEntry = zip.getEntry("meta.json")
                ?: error("meta.json is not existed")
            val metaText = zip
                .getInputStream(metaEntry)
                .bufferedReader()
                .use {
                    it.readText()
                }

            val meta = Json.Default.decodeFromString<PatchWithMeta>(metaText)

            val diffEntry = zip.getEntry("diff.bin")
                ?: error("diff.bin is not existed")

            val diffOutFile = File(
                getPatchCacheDir(context),
                "diff_${System.currentTimeMillis()}.bin"
            )

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
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // 将源Apk复制到工作目录
    private fun copySourceApk(context: Context): File? = copySourceApkTo(context,getPatchCacheDir(context))

    // 检查是否适用当前版本并且Patch完整 完整则返回Source的File
    private fun checkPatch(patchContent : PatchWithMetaContent, context : Context) : DiffResult {
        // 复制源Apk
        val sourceApk = copySourceApk(context)
            ?: return DiffResult.Error(
                DiffError(
                    DiffErrorCode.SOURCE_APK_NOT_FOUND,
                    "Not found copied apk in environment"
                )
            )

        val sourceMd5 = getMd5(sourceApk)
        val patchMd5 = patchContent.meta.source.md5

        if (sourceMd5 != patchMd5) {
            return DiffResult.Error(
                DiffError(
                    DiffErrorCode.MD5_MISMATCH,
                    "Check source md5 expected $patchMd5 but actually $sourceMd5"
                )
            )
        }

        if (!patchContent.diffFile.exists()) {
            return DiffResult.Error(
                DiffError(
                    DiffErrorCode.DIFF_FILE_NOT_FOUND,
                    "Not found diff.bin"
                )
            )
        }

        return DiffResult.Success(sourceApk)
    }

    // 校验合成包
    private fun checkTarget(targetFile: File, patchContent: PatchWithMetaContent): Boolean {
        if (!targetFile.exists()) {
            return false
        }
        val md5 = getMd5(targetFile)
        return md5 == patchContent.meta.target.md5
    }

    // 清理工作目录
    private fun clean(context: Context, exclude: File? = null) {
        val dir = getPatchCacheDir(context)
        dir.listFiles()?.forEach {
            if (it != exclude) {
                it.delete()
            }
        }
    }

    // 清理工作目录 建议安装完成后，调用
    fun clean(context: Context) = clean(context,null)

    // 合并补丁包
    suspend fun merge (
        patchFile: File,
        context : Context,
    ) : DiffResult = withContext(Dispatchers.IO) {
        // 解析Patch文件
        val patchContent = parsePatchFile(patchFile, context)
        // 检查Patch文件 并复制本体Apk到工作目录，然后返回Apk的File
        val sourceApkResult = checkPatch(patchContent, context)
        if (sourceApkResult is DiffResult.Error) {
            clean(context)
            return@withContext sourceApkResult
        }
        // 合并文件的叫target.apk 若放在工作目录，则需要安装完成后清理缓存
        val targetFile = File(
            getPatchCacheDir(context),
            "${getPackageName(context)}_target.apk"
        )
        // 确认两个文件还在
        val diffFile = patchContent.diffFile
        if(!diffFile.exists()) {
            return@withContext DiffResult.Error(
                DiffError(
                    DiffErrorCode.DIFF_FILE_NOT_FOUND,
                    "Diff not found in environment"
                )
            )
        }
        val sourceApk = (sourceApkResult as DiffResult.Success).file
        if(!sourceApk.exists()) {
            return@withContext DiffResult.Error(
                DiffError(
                    DiffErrorCode.SOURCE_APK_NOT_FOUND,
                    "Not found source apk in environment"
                )
            )
        }
        // 合并
        val mergeResult = bsdiff.merge(
            sourceApk.absolutePath,
            patchContent.diffFile.absolutePath,
            targetFile.absolutePath
        )
        // 合并结果
        if(mergeResult != 0) {
            return@withContext DiffResult.Error(
                DiffError(
                    DiffErrorCode.MERGE_FAILED,
                    "Merged failed with code $mergeResult"
                )
            )
        }
        // 进行校验
        val checkResult = checkTarget(targetFile, patchContent)
        if (!checkResult) {
            clean(context)
            return@withContext DiffResult.Error(
                DiffError(
                    DiffErrorCode.MD5_MISMATCH,
                    "Check md5 expected ${patchContent.meta.source.md5} but actually ${getMd5(targetFile)}"
                )
            )
        }
        // 清理工作目录 新合成文件除外
        clean(context, targetFile)
        return@withContext DiffResult.Success(targetFile)
    }

    // 合并补丁包回调版本
    suspend fun mergeCallback (
        patchFile: File,
        context : Context,
        onResult : (DiffResult) -> Unit = { result ->
            when(result) {
                is DiffResult.Success -> {
                    val targetFile = result.file
                    // 安装
                    installApk(context, targetFile) {
                        Toast.makeText(context,"Not found target apk to install", Toast.LENGTH_SHORT).show()
                    }
                }
                is DiffResult.Error -> {
                    // 错误
                    val error = result.error
                    Toast.makeText(context,error.message, Toast.LENGTH_SHORT).show()
                }
            }
        },
    )  = withContext(Dispatchers.IO) {
        val targetFile = merge(patchFile, context)
        // 允许开发者定制成功后的操作 调用安装
        withContext(Dispatchers.Main) {
            onResult(targetFile)
        }
    }
}