# Patch-Android-Library   [![](https://jitpack.io/v/Chiu-xaH/Bsdiff-Android-Library.svg)](https://jitpack.io/#Chiu-xaH/Bsdiff-Android-Library)

适用于Android的库，集成了增量更新功能，需要开发者将生成的补丁包，推送给用户下载，下载到 内部存储/Download文件夹 后，调用mergePatchApk即可完成合并及其安装

## [增量包生成GUI工具 (Windows x86_64)](https://github.com/Chiu-xaH/Bsdiff-Tool)

## 示例
![图片](/img/a.png)

实际应用案例：[聚在工大](https://github.com/Chiu-xaH/HFUT-Schedule/releases)中安装最新版本的近期旧版本的**ARM64位**APK，在APP内即可使用增量更新，[视频演示](/img/example.mp4)

## 引入
在settings.gradle添加
```Groovy
maven { url 'https://jitpack.io' }
```

添加依赖，版本以Tag为准
```Groovy
implementation("XXX")
```

## 快速开始
1. 为保证安装Apk的顺利进行，需先配置FileProvider
新建res/xml/file_paths.xml，添加如下路径（库在data中的cache文件夹进行新安装包的生成）
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!-- 允许访问私有缓存目录 -->
    <cache-path
        name="cache"
        path="." />
</paths>
```
在AndroidManifest.xml中配置ContentProvider：
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```
2. 懒人接入：直接传补丁包的java.File，调用如下函数即可从合并到安装Apk 
```Kotlin
DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback(it, context)
```
**如需自定义请继续向下看**

3. 实例化DiffUpdate类

要求传入DiffType，分别为BSDIFF,H_DIFF_PATCH，根据补丁包的来源选择，推荐H_DIFF_PATCH

4. 调用合并函数
合并函数有如下：
```Kotlin
// 基础函数，传入DiffContent和Context，对传入的补丁包与本体Apk进行合并并校验MD5，完成后返回DiffResult
suspend fun merge (
    diffContent: DiffContent,
    context : Context
) : DiffResult
```
DiffContent定义如下，包含目标文件的MD5和补丁包的java.File。若传入MD5为null，则直接跳过MD5校验
```Kotlin
data class DiffContent(
    val targetFileMd5 : String?,
    val diffFile: File
)
```
DiffResult定义如下，会返回以下两种结果：若成功生成新Apk则返回其java.File，否则给出错误
```Kotlin
sealed class DiffResult {
    data class Success(val file: File) : DiffResult()
    data class Error(val error: DiffError) : DiffResult()
}
// 错误
data class DiffError(
    val code: DiffErrorCode,
    val message: String
)
// 常见错误类型代号
enum class DiffErrorCode(val code: Int) {
    SOURCE_APK_NOT_FOUND(1000),// 源Apk在工作目录内未找到，可能是复制Apk时出现问题
    DIFF_FILE_NOT_FOUND(1001), // 补丁包未找到，可能是被删除了
    MERGE_FAILED(1002), // Native层合并失败，原因将以代号的形式置于message中
    MD5_MISMATCH(1003), // MD5校验不通过
}
```
上面是基础merge函数，开发者可以调用后自行处理报错和成功后的操作。**库内也为其预设了一些合并函数**
```Kotlin
// 回调版merge
suspend fun mergeCallback (
    diffContent: DiffContent,
    context : Context,
    onResult : (DiffResult) -> Unit = { result ->
        mergedDefaultFunction(result,context) 
    }, 
)
// 不校验MD5的回调版merge
suspend fun mergeCallback (
    diffFile: File,
    context : Context,
    onResult : (DiffResult) -> Unit = { result ->
        mergedDefaultFunction(result,context)
    },
)
// 不校验MD5的merge
suspend fun merge (
    diffFile: File,
    context : Context,
) : DiffResult
```
5. 处理合并完成后的操作

预设的mergedDefaultFunction函数是库预设的回调处理，失败时使用Log.e打印并Toast，成功时安装新Apk
```Kotlin
// 合并完成后的默认操作
fun mergedDefaultFunction(
    result : DiffResult,
    context: Context,
    authority : String = ".provider",
) {
    when(result) {
        is DiffResult.Success -> {
            val targetFile = result.file
            // 安装
            installApk (targetFile,context,authority) {
                Toast.makeText(context,"Not found target apk to install", Toast.LENGTH_SHORT).show()
            }
        }
        is DiffResult.Error -> {
            // 错误
            val error = result.error
            Log.e("DiffUpdate","code: " + error.code + "\nmessage: " + error.message)
            Toast.makeText(context,error.message, Toast.LENGTH_SHORT).show()
        }
    }
}
```
6. 最终搭配Compose的文件选择器，完整使用如下：
```Kotlin
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
        uri?.let { 
            val name = queryName(context.contentResolver, uri)
            if (name?.endsWith(".patch") == true) {
                val patchFile = uriToFile(context, uri)
                patchFile?.let {
                    scope.launch {
                        DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback(it, context)
                    }
                }
            } else {
                Toast.makeText(context, "请选择 .patch 文件", Toast.LENGTH_SHORT).show()
            }
        }
    }
)
```
7. **缓存清理**

DiffUpdate提供了静态方法clean，用于工作目录下的所有文件
```Kotlin
DiffUpdate.clean(context)
```
调用merge时会先清理工作目录；当合并失败时，会清理掉工作目录；合并成功时，会清理掉除新Apk以外的文件缓存；

但是由于安装Apk后，已经杀死了App，无法清除安装包缓存，但一些国内定制UI系统都会帮助清理，而且本身就在cache文件夹，可以被系统当缓存清理掉，当然开发者也可以调用clean函数进行清理。 

7. **从网络下载文件**

库也为其提供了一个异步的downloadFile函数，借助系统的DownloadManager，返回Flow
```Kotlin
fun downloadFile(
    context: Context,
    url: String,
    fileName: String,
    delayTimesLong: Long = 1000L,
    requestBuilder: (DownloadManager.Request) -> DownloadManager.Request = { it },
    customDownloadId: Long? = null
): Flow<DownloadResult>

// 初始化 获取下载文件的大小、判断是否下载过
suspend fun initDownloadFileStatus(
    url: String,
    fileName: String,
    timeOutTime: Int = 5000
) : DownloadResult

// 获取文件bytes
suspend fun getFileSize(
    url: String,
    timeOutTime : Int = 5000
): Long?
```
其中DownloadResult为返回结果，初始状态可通过initDownloadFileStatus获取，或者直接为Prepare，定义如下：
```Kotlin
sealed class DownloadResult {
    // 下载目录中已经有此文件，直接返回java.File
    data class Downloaded(val file : File) : DownloadResult()
    // 正在下载，progress为下载进度，从0~100，每delayTimesLong毫秒更新一次
    data class Progress(val downloadId: Long, val progress: Int) : DownloadResult()
    // 下载完成，可对File或Uri进行处理，例如Uri可以安装Apk，库内预设了此方法，InstallUtils.installApk()
    data class Success(val downloadId: Long, val file: File, val uri: Uri) : DownloadResult()
    // 下载失败，会打印Log并且给出可能的原因
    data class Failed(val downloadId: Long, val reason: String?) : DownloadResult()
    // 准备状态，未启动下载，这时可以告知用户文件的大小（可选）
    data class Prepare(val fileSize : Long? = null) : DownloadResult()
}
```
完整使用示例如下：
```Kotlin
class UpdateViewModel() : ViewModel() {
    private val _downloadState = MutableStateFlow<DownloadResult>(
        DownloadResult.Prepare
    )
    val downloadState: StateFlow<DownloadResult> = _downloadState

    private var downloadJob: Job? = null

    fun startDownload(url : String,filename : String,context: Context) {
        // 避免重复下载
        if (downloadJob != null) return

        downloadJob = viewModelScope.launch {
            downloadFile(
                context = context,
                url = url,
                fileName = filename
            ).collect { result ->
                _downloadState.value = result
            }
        }
    }

    fun reset() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadResult.Prepare
    }
}
// UI
@Composable
fun PatchUpdateUI(
    viewModel: UpdateViewModel = viewModel<UpdateViewModel>(key = "patch")
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loadingPatch by remember { mutableStateOf(false) }
    val downloadState by viewModel.downloadState.collectAsState()

    when (downloadState) {
        is DownloadResult.Prepare -> {
            // 准备阶段
            LargeButton(
                onClick = {
                    viewModel.startDownload(下载链接, 文件名, context)
                },
                text = "下载(${文件大小}MB)",
            )
        }
        is DownloadResult.Downloaded -> {
            // 检查是否有下载好的文件 有就显示
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = APP_HORIZONTAL_DP), horizontalArrangement = Arrangement.Center)  {
                LargeButton(
                    onClick = {
                        scope.launch {
                            loadingPatch = true
                            // 合并并安装
                            DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback((downloadState as DownloadResult.Downloaded).file, context)
                            loadingPatch = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().weight(1/2f),
                    text = "安装",
                )
                LargeButton(
                    onClick = {
                        (downloadState as DownloadResult.Downloaded).file.delete()
                        viewModel.reset()
                    },
                    modifier = Modifier.fillMaxWidth().weight(1/2f),
                    text = "删除",
                )
            }
        }
        is DownloadResult.Progress -> {
            // 更新进度
            Text("${(downloadState as DownloadResult.Progress).progress}%")
        }
        is DownloadResult.Success -> {
            LargeButton(
                onClick = {
                    scope.launch {
                        loadingPatch = true
                        // 合并并安装
                        DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback((downloadState as DownloadResult.Success).file, context)
                        loadingPatch = false
                    }
                },
                text = "安装",
            )
        }
        is DownloadResult.Failed -> {
            LargeButton(
                onClick = {
                    viewModel.reset()
                },
                text = "重试",
            )
        }
    }
}
```