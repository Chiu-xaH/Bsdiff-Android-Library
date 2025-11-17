package com.xah.patch.diff

import android.content.Context
import android.widget.Toast
import com.bsdiff.core.BsdiffJni
import com.xah.patch.diff.model.DiffContent
import com.xah.bsdiffs.result.DiffError
import com.xah.bsdiffs.result.DiffErrorCode
import com.xah.bsdiffs.result.DiffResult
import com.xah.shared.util.InstallUtils.installApk
import com.xah.shared.util.copySourceApkTo
import com.xah.shared.util.getMd5
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
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // 将源Apk复制到工作目录
    private fun copySourceApk(context: Context): File? = copySourceApkTo(context,getPatchCacheDir(context))
    // 校验合成包
    private fun checkTarget(targetFile: File, diffContent: DiffContent): Boolean {
        if (!targetFile.exists()) {
            return false
        }
        val md5 = getMd5(targetFile)
        return md5 == diffContent.targetFileMd5
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
        diffContent: DiffContent,
        context : Context,
    ) : DiffResult = withContext(Dispatchers.IO) {
        // 复制本体Apk到工作目录
        val sourceApk = copySourceApk(context)
            ?: return@withContext DiffResult.Error(
                DiffError(
                    DiffErrorCode.SOURCE_APK_NOT_FOUND,
                    "Not found copied apk in environment"
                )
            )
        // 合并文件的叫target.apk 若放在工作目录，则需要安装完成后清理缓存
        val targetFile = File(
            getPatchCacheDir(context),
            "${context.packageName}_target.apk"
        )
        // 确认两个文件还在
        val diffFile = diffContent.diffFile
        if(!diffFile.exists()) {
            return@withContext DiffResult.Error(
                DiffError(
                    DiffErrorCode.DIFF_FILE_NOT_FOUND,
                    "Diff not found in environment"
                )
            )
        }
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
            diffContent.diffFile.absolutePath,
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
        val checkResult = checkTarget(targetFile, diffContent)
        if (!checkResult) {
            clean(context)
            return@withContext DiffResult.Error(
                DiffError(
                    DiffErrorCode.MD5_MISMATCH,
                    "Check md5 expected ${diffContent.targetFileMd5} but actually ${getMd5(targetFile)}"
                )
            )
        }
        // 清理工作目录 新合成文件除外
        clean(context, targetFile)
        return@withContext DiffResult.Success(targetFile)
    }

    // 合并补丁包回调版本
    suspend fun mergeCallback (
        diffContent: DiffContent,
        context : Context,
        onResult : (DiffResult) -> Unit = { result ->
            when(result) {
                is DiffResult.Success -> {
                    val targetFile = result.file
                    // 安装
                    installApk (targetFile,context) {
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
    ) = withContext(Dispatchers.IO) {
        val targetFile = merge(diffContent, context)
        // 允许开发者定制成功后的操作 调用安装
        withContext(Dispatchers.Main) {
            onResult(targetFile)
        }
    }
}