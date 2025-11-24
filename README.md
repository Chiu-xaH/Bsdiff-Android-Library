# DiffUpdater (Android Library) 

[![](https://jitpack.io/v/Chiu-xaH/Bsdiff-Android-Library.svg)](https://jitpack.io/#Chiu-xaH/Bsdiff-Android-Library)

[English](README_en.md) | 中文

适用于Android的库，集成了增量更新功能，开发者将补丁包的java.io.File（后续统称File）传入即可完成合并及安装

## 实际应用案例
[聚在工大](https://github.com/Chiu-xaH/HFUT-Schedule/releases)中安装最新版本的近期旧版本的ARM64位APK，[视频演示](/img/example.mp4)

![图片](/img/a.png)

## [增量包GUI工具 (Windows x86_64)](https://github.com/Chiu-xaH/Bsdiff-Tool)

## 快速开始
### 引入依赖
在settings.gradle添加
```Groovy
maven { url 'https://jitpack.io' }
```
添加依赖，版本以Tag为准
```Groovy
implementation("com.github.Chiu-xaH:diff-updater:XXX")
```

### 配置FileProvider
为保证安装Apk的顺利进行，需先配置FileProvider。

新建res/xml/file_paths.xml，添加如下路径：
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!-- 允许访问公有目录 -->
    <external-path name="external" path="." />
    <!-- 允许访问私有缓存目录 增量更新的工作目录在此处 -->
    <cache-path name="cache" path="." />
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

### 给予安装Apk权限
在AndroidManifest.xml添加权限，无需在代码中申请权限，而且有时候不需要这个权限也能安装，但最好加上。
```xml
<manifest>
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
</manifest>
```

### 给予存储权限
如果文件涉及公有目录（例如下载文件的Download），需要授权存储权限；当然，开发者可以把文件放在私有目录，就可以跳过此步了

在AndroidManifest.xml添加下面内容，并在代码中根据API版本申请权限

API>=30申请新的存储权限
```xml
<manifest>
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
</manifest>
```
API<30申请旧的读与存权限
```xml
<manifest>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
</manifest>
```
API=29需额外添加到application
```xml
<application
    android:requestLegacyExternalStorage="true">
</application>
```

代码中动态申请存储权限示例
```Kotlin
fun checkAndRequestStoragePermission(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${activity.packageName}".toUri()
                activity.startActivityForResult(intent, 1)
            } catch (e: Exception) {
                // 某些手机拉不出来 , 使用全局设置页面
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivityForResult(intent, 1)
            }
        }
    } else {
        // Android 10 及以下
        val needReq = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).any {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needReq) {
            ActivityCompat.requestPermissions(activity, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1)
        }
    }
}
```

### 合并补丁包
传补丁包的File(**如果File在公有目录，需提前申请存储权限**)，调用如下函数即可合并并安装Apk（预设操作），如需自定义，请继续向下阅读 
```Kotlin
DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback(it, context)
```

#### 自定义
实例化DiffUpdate类时，要求传入DiffType，分别为BSDIFF,H_DIFF_PATCH，根据上层补丁包的来源选择，推荐H_DIFF_PATCH

DiffUpdate类有若干合并补丁包函数，其中第一个为基础函数，开发者可以调用后自行处理报错和成功后的操作。其余三个函数为预设，均基于第一个再封装。
```Kotlin
class DifferUpdate(private val differType : DifferType) {
    // 基础函数，传入DiffContent和Context，对传入的补丁包与本体Apk进行合并并校验MD5，完成后返回DiffResult
    suspend fun merge (
        diffContent: DiffContent,
        context : Context
    ) : DiffResult
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
}
```
DiffContent定义如下，包含目标文件的MD5和补丁包的File。若传入MD5为null，则直接跳过MD5校验
```Kotlin
data class DiffContent(val targetFileMd5 : String?, val diffFile: File)
```
DiffResult含Success、Error两种结果：若成功生成新Apk则返回其File，否则给出错误信息
```Kotlin
// 常见错误类型代号
enum class DiffErrorCode(val code: Int) {
    SOURCE_APK_NOT_FOUND(1000),// 源Apk在工作目录内未找到，可能是复制Apk时出现问题
    DIFF_FILE_NOT_FOUND(1001), // 补丁包未找到，可能是被删除了
    MERGE_FAILED(1002), // Native层合并失败，原因将以代号的形式置于message中
    MD5_MISMATCH(1003), // MD5校验不通过
}
```
mergedDefaultFunction函数是库预设的回调处理，失败时使用Log.e打印并Toast，成功时安装新Apk
```Kotlin
// 合并完成后的默认操作
fun mergedDefaultFunction(
    result : DiffResult,
    context: Context,
    authority : String = ".provider",
) 
```

#### 缓存清理
DiffUpdate提供了静态方法clean，用于手动清理工作目录
```Kotlin
DiffUpdate.clean(context)
```
即使开发者不手动清理，调用merge函数会先清理工作目录；当合并失败时，会清理掉工作目录；合并成功时，会清理掉除新Apk以外的文件缓存；

clean永远只能清理工作目录，补丁包从外部传入不会被清理，需要开发者在merge成功后手动清理，这个不难，开发者既然能传入File，就可以调用delete()删掉；

由于安装Apk后，已经杀死了App，无法清除安装包缓存，但一些安装器支持安装后自动删除Apk，而且本身就在cache文件夹，可以被系统当缓存清理掉；当然开发者也可以调用clean函数进行清理，建议每次更新后调用。

#### 使用示例
最终搭配Compose的文件选择器，使用示例如下：
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

### 安装Apk
注意配置好FileProvider，传入Uri或者File（**如果File在公有目录，需提前申请存储权限**）安装Apk，推荐传入Uri（下载完成后会返回Uri）
```Kotlin
object InstallUtils {
    fun installApk(
        apkFile : File,
        context: Context,
        authority : String = ".provider",
        onNotFound : (() -> Unit)? = null,
    )

    fun installApk(
        uri : Uri,
        context : Context
    ) 
}
```

### 下载文件
库也为其提供了一个异步、基于Flow、借助DownloadManager的downloadFile函数，用于下载补丁包或安装包。

```Kotlin
object DownloadUtils {
    // 获取文件bytes
    suspend fun getFileSize(
        url: String,
        timeOutTime : Int = 5000
    ): Long?

    // 初始化 获取下载文件的大小、判断是否下载过(可选校验MD5)
    fun initDownloadFileStatus(
        fileName: String,
        destDir : File? = null,
        fileMd5 : String?,
        fileSize : Long?,
    ) : DownloadResult

    /**
     * @param context 上下文
     * @param url 下载链接
     * @param fileName 文件名，记得带扩展名
     * @param fileMd5 期望MD5，用于检验是否下载过或者下载完成后是否符合期望
     * @param destDir 下载目录，默认为null代表下载到公有Download目录(需存储权限)
     * @param delayTimesLong 下载进度更新间隔，单位为毫秒
     * @param requestBuilder 自定义下载器构建
     * @param customDownloadId 自定义下载ID，默认为null代表由系统分配，下载过程会返回id
     */
    fun downloadFile(
        context: Context,
        url: String,
        fileName: String,
        fileMd5 : String? = null, 
        destDir: File? = null,
        delayTimesLong: Long = 1000L,
        requestBuilder: (DownloadManager.Request) -> DownloadManager.Request = { it },
        customDownloadId: Long? = null
    ): Flow<DownloadResult>
}
```
其中DownloadResult为返回结果，初始状态可通过initDownloadFileStatus获取，或者直接为Prepare，定义如下：
```Kotlin
sealed class DownloadResult {
    // 下载目录中已经有此文件，直接返回File
    data class Downloaded(val file : File) : DownloadResult()
    // 正在下载，progress为下载进度，从0~100，每delayTimesLong毫秒更新一次
    data class Progress(val downloadId: Long, val progress: Int) : DownloadResult()
    // 下载完成，可对File或Uri进行处理，例如Uri可以安装Apk，库内预设了此方法，InstallUtils.installApk()
    data class Success(val downloadId: Long, val file: File, val uri: Uri, val checked : Boolean) : DownloadResult()
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
                    viewModel.startDownload("下载链接", "文件名", context)
                },
                text = "下载(${"文件大小"}MB)",
            )
        }
        is DownloadResult.Downloaded -> {
            // 检查是否有下载好的文件 有就显示
            LargeButton(
                onClick = {
                    scope.launch {
                        loadingPatch = true
                        // 合并并安装
                        DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback((downloadState as DownloadResult.Downloaded).file, context)
                        loadingPatch = false
                    }
                },
                text = "安装",
            )
            LargeButton(
                onClick = {
                    (downloadState as DownloadResult.Downloaded).file.delete()
                    viewModel.reset()
                },
                text = "删除",
            )
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

## 声明
尽管库已经做了报错封装等举措，并在不同的SDK版本进行测试，但由于测试设备以及开发经验有限，仍不可避免会出现问题。
### 兼容性（基于Android Studio模拟器）

| API | 下载文件 | 安装Apk | 合并增量包 |
|-----|------|-------|-------|
| 24  | √    | √     | 待测    |
| 25  | 待测   | 待测    | 待测    |
| 26  | √    | √     | 待测    |
| 27  | 待测   | 待测    | 待测    |
| 28  | 待测   | 待测    | 待测    |
| 29  | √    | √     | 待测    |
| 30  | 待测   | 待测    | 待测    |
| 31  | √    | √     | 待测    |
| 32  | 待测   | 待测    | 待测    |
| 33  | 待测   | 待测    | 待测    |
| 34  | √    | √     | √     |
| 35  | √    | √     | √     |
| 36  | 待测   | 待测    | 待测    |

### 定制
如需单独定制，开发者可以单独引入**core**模块，此模块只有Native层，然后自行自定义。
### 开源致谢
- Bsdiff
- HPatchDiff
### 应用升级系统与应用分发平台
👉 [Upgradelink](https://github.com/toolsetlink/upgradelink)