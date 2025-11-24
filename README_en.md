# Patch-Android-Library  

[![](https://jitpack.io/v/Chiu-xaH/Bsdiff-Android-Library.svg)](https://jitpack.io/#Chiu-xaH/Bsdiff-Android-Library)

English | [中文](README_en.md)

A library for Android that integrates incremental update functionality. Developers only need to pass the patch package as a `java.io.File`(hereinafter collectively referred to as “File”) to complete the merge and installation.

Real-world usage example: In [HFUT-Schedule](https://github.com/Chiu-xaH/HFUT-Schedule/releases), you can install updates for older **ARM64 APKs** directly through incremental updates inside the app.
[Video Demo](/img/example.mp4)

![Image](/img/a.png)

## [GUI Tool for Patch Generation (Windows x86_64)](https://github.com/Chiu-xaH/Bsdiff-Tool)

## Quick Start

### Add Dependency

Add the following in `settings.gradle`:

```Groovy
maven { url 'https://jitpack.io' }
```

Add dependency (use Tag version):

```Groovy
implementation("XXX-patch")
```

### Configure FileProvider

To ensure APK installation works properly, you must configure FileProvider.

Create `res/xml/file_paths.xml` and add:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!-- Allow access to public directories such as Download -->
    <external-path name="external" path="." />
    <!-- Allow access to private cache directory where patching works -->
    <cache-path name="cache" path="." />
</paths>
```

Add FileProvider to your `AndroidManifest.xml`:

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

### Merge Patch Package

Pass a File patch file and call the following method to merge and install the APK (default behavior).
For customization, continue reading below.

```Kotlin
DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback(it, context)
```

#### Customization

When instantiating `DiffUpdate`, you must pass a `DiffType`, which can be `BSDIFF` or `H_DIFF_PATCH`.
It is recommended to use `H_DIFF_PATCH`.

`DiffUpdate` provides several merge functions. The first is the base function; developers can handle success and error manually.
The others are presets built on top of the base function.

```Kotlin
class DifferUpdate(private val differType : DifferType) {
    // Base function: merge patch and verify MD5, returns DiffResult
    suspend fun merge (
        diffContent: DiffContent,
        context : Context
    ) : DiffResult

    // Callback version of merge
    suspend fun mergeCallback (
        diffContent: DiffContent,
        context : Context,
        onResult : (DiffResult) -> Unit = { result ->
            mergedDefaultFunction(result,context)
        },
    )

    // Callback version without MD5 verification
    suspend fun mergeCallback (
        diffFile: File,
        context : Context,
        onResult : (DiffResult) -> Unit = { result ->
            mergedDefaultFunction(result,context)
        },
    )

    // Merge without MD5 verification
    suspend fun merge (
        diffFile: File,
        context : Context,
    ) : DiffResult
}
```

`DiffContent` is defined as follows and contains the target file MD5 and the patch file.
If `MD5` is `null`, the verification step is skipped:

```Kotlin
data class DiffContent(val targetFileMd5 : String?, val diffFile: File)
```

`DiffResult` contains two result types: `Success` or `Error`.
On success, the new APK file is returned; on failure, an error message is provided:

```Kotlin
// Common error codes
enum class DiffErrorCode(val code: Int) {
    SOURCE_APK_NOT_FOUND(1000), // Source APK not found in workspace
    DIFF_FILE_NOT_FOUND(1001),  // Patch file not found
    MERGE_FAILED(1002),         // Native merge failed
    MD5_MISMATCH(1003),         // MD5 check failed
}
```

`mergedDefaultFunction` is the library’s default callback behavior:
On failure, it logs and shows a Toast.
On success, it installs the new APK.

```Kotlin
fun mergedDefaultFunction(
    result : DiffResult,
    context: Context,
    authority : String = ".provider",
)
```

#### Cache Cleanup

`DiffUpdate` provides a static method to clear the working directory:

```Kotlin
DiffUpdate.clean(context)
```

Behavior:

* Workspace is cleaned before merging.
* On failure, workspace is cleaned.
* On success, all files except the generated APK are cleaned.
* The installed APK cache remains (app process is killed), but many OEM ROMs clean it automatically; developers may also call `clean()` manually.

#### Usage Example

Paired with a Compose file picker:

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
                Toast.makeText(context, "Please select a .patch file", Toast.LENGTH_SHORT).show()
            }
        }
    }
)
```

---

### APK Installation

Be sure FileProvider is configured correctly.
You may install via `Uri` or `File` (Uri is recommended, especially after downloads).

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

---

### Download Files

The library provides an asynchronous, Flow-based `downloadFile` method using `DownloadManager`.

```Kotlin
fun downloadFile(
    context: Context,
    url: String,
    fileName: String,
    delayTimesLong: Long = 1000L,
    requestBuilder: (DownloadManager.Request) -> DownloadManager.Request = { it },
    customDownloadId: Long? = null
): Flow<DownloadResult>
```

Initialize to get file size or check if it already exists:

```Kotlin
suspend fun initDownloadFileStatus(
    url: String,
    fileName: String,
    timeOutTime: Int = 5000
) : DownloadResult
```

Get remote file size:

```Kotlin
suspend fun getFileSize(
    url: String,
    timeOutTime : Int = 5000
): Long?
```

`DownloadResult` is defined below:

```Kotlin
sealed class DownloadResult {
    data class Downloaded(val file : File) : DownloadResult()
    data class Progress(val downloadId: Long, val progress: Int) : DownloadResult()
    data class Success(val downloadId: Long, val file: File, val uri: Uri) : DownloadResult()
    data class Failed(val downloadId: Long, val reason: String?) : DownloadResult()
    data class Prepare(val fileSize : Long? = null) : DownloadResult()
}
```

Complete usage example:
*(kept exactly as original)*

```Kotlin
class UpdateViewModel() : ViewModel() {
    private val _downloadState = MutableStateFlow<DownloadResult>(
        DownloadResult.Prepare
    )
    val downloadState: StateFlow<DownloadResult> = _downloadState

    private var downloadJob: Job? = null

    fun startDownload(url : String,filename : String,context: Context) {
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
            LargeButton(
                onClick = {
                    viewModel.startDownload("DownloadUrl", "FileName", context)
                },
                text = "Download (${"FileSize"}MB)",
            )
        }
        is DownloadResult.Downloaded -> {
            LargeButton(
                onClick = {
                    scope.launch {
                        loadingPatch = true
                        DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback((downloadState as DownloadResult.Downloaded).file, context)
                        loadingPatch = false
                    }
                },
                text = "Install",
            )
            LargeButton(
                onClick = {
                    (downloadState as DownloadResult.Downloaded).file.delete()
                    viewModel.reset()
                },
                text = "Delete",
            )
        }
        is DownloadResult.Progress -> {
            Text("${(downloadState as DownloadResult.Progress).progress}%")
        }
        is DownloadResult.Success -> {
            LargeButton(
                onClick = {
                    scope.launch {
                        loadingPatch = true
                        DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback((downloadState as DownloadResult.Success).file, context)
                        loadingPatch = false
                    }
                },
                text = "Install",
            )
        }
        is DownloadResult.Failed -> {
            LargeButton(
                onClick = {
                    viewModel.reset()
                },
                text = "Retry",
            )
        }
    }
}
```
## Disclaimer
Although I have added error handling and tested the library across different SDK versions, issues may still occur due to limitations in my testing devices and development experience. If customization is needed, developers may import the **core** module separately — this module contains only the native layer — and implement their own integration logic.

Open Source Acknowledgments:
- Bsdiff
- HPatchDiff
