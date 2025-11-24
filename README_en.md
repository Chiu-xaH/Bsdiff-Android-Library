# DiffUpdater (Android Library)

[![Jitpack version](https://jitpack.io/v/Chiu-xaH/Bsdiff-Android-Library.svg)](https://jitpack.io/#Chiu-xaH/Bsdiff-Android-Library)

English | [中文](README.md)

An Android library that integrates incremental update functionality. Developers simply pass the patch package's `java.io.File` (hereafter referred to as File) to merge and install it.

## Practical Application Case

Install the latest version of a recent old version of an ARM64 APK in [HFUT Schedule](https://github.com/Chiu-xaH/HFUT-Schedule/releases) ([Video Demo](/img/example.mp4))

![Example Image](/img/a.png)

## Quick Start

### Generating and Distributing Incremental Packages

#### [GUI Tool](https://github.com/Chiu-xaH/Bsdiff-Tool) (Windows x86_64)

#### [UpgradeLink](https://github.com/toolsetlink/upgradelink) (App upgrade system and distribution platform)

#### [HDiffPatch](https://github.com/sisong/HDiffPatch) (Use with the `-f` parameter)

### Add Dependency

In `settings.gradle` add:

Groovy
```Groovy
maven { 
    url 'https://jitpack.io'
}
```
Kotlin
```Kotlin
maven {
    url = uri("https://jitpack.io")
}
```

Add dependency (version is based on the Tag):

```Groovy
implementation("com.github.Chiu-xaH:DiffUpdater:2.0-dev01")
```

### Configure FileProvider

To ensure the smooth installation of the APK, configure the `FileProvider` first.

Create `res/xml/file_paths.xml` and add the following paths:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!-- Allow access to public directories -->
    <external-path name="external" path="." />
    <!-- Allow access to private cache directories where incremental update working directories are stored -->
    <cache-path name="cache" path="." />
</paths>
```

In `AndroidManifest.xml`, configure the `ContentProvider`:

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

### Install APK Permissions

Add permission in `AndroidManifest.xml` to install the APK. Although this may not always be necessary, it's best to add it.

```xml
<manifest>
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
</manifest>
```

### Storage Permissions

If the file involves public directories (such as downloaded files in `Download`), storage permissions need to be granted. However, developers can choose to store the file in a private directory to bypass this step.

Add the following in `AndroidManifest.xml`, and request permissions in the code based on the API version.

For API >= 30, request new storage permissions:

```xml
<manifest>
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
</manifest>
```

For API < 30, request old read/write storage permissions:

```xml
<manifest>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
</manifest>
```

For API = 29, add the following in the `application` tag:

```xml
<application
    android:requestLegacyExternalStorage="true">
</application>
```

Example of dynamically requesting storage permissions in code:

```Kotlin
fun checkAndRequestStoragePermission(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${activity.packageName}".toUri()
                activity.startActivityForResult(intent, 1)
            } catch (e: Exception) {
                // Some phones may not allow, use global settings page
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivityForResult(intent, 1)
            }
        }
    } else {
        // For Android 10 and below
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

### Merge Patch Package

Pass the patch file (`File`) (if the file is in a public directory, ensure storage permissions are granted first) and call the following function to merge and install the APK (default behavior). For customization, refer to the following details:

```Kotlin
DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback(it, context)
```

#### Customization

When instantiating the `DiffUpdate` class, you must specify the `DiffType`, either `BSDIFF` or `H_DIFF_PATCH`. It is recommended to use `H_DIFF_PATCH` based on the patch package source.

The `DiffUpdate` class has several patch merge functions. The first one is the base function, and developers can handle errors and success operations themselves. The remaining three functions are predefined, built on the first one.

```Kotlin
class DifferUpdate(private val differType : DifferType) {
    // Base function, pass DiffContent and Context to merge the patch with the APK and validate MD5, returning DiffResult
    suspend fun merge (
        diffContent: DiffContent,
        context : Context
    ) : DiffResult
    // Callback version of merge
    suspend fun mergeCallback (
        diffContent: DiffContent,
        context : Context,
        onResult : (DiffResult) -> Unit = { result -> 
            mergedDefaultFunction(result, context)
        },
    )
    // Callback version of merge without MD5 validation
    suspend fun mergeCallback (
        diffFile: File,
        context : Context,
        onResult : (DiffResult) -> Unit = { result -> 
            mergedDefaultFunction(result, context)
        },
    )
    // Merge without MD5 validation
    suspend fun merge (
        diffFile: File,
        context : Context,
    ) : DiffResult
}
```

The `DiffContent` definition includes the target file's MD5 and the patch file's `File`. If MD5 is null, the MD5 check will be skipped.

```Kotlin
data class DiffContent(val targetFileMd5 : String?, val diffFile: File)
```

`DiffResult` has two possible outcomes: `Success` or `Error`. If the APK is successfully generated, its `File` is returned; otherwise, an error message is provided.

```Kotlin
// Common error codes
enum class DiffErrorCode(val code: Int) {
    SOURCE_APK_NOT_FOUND(1000),// Source APK not found in working directory, possible issue during APK copying
    DIFF_FILE_NOT_FOUND(1001), // Patch file not found, possibly deleted
    MERGE_FAILED(1002), // Native layer merge failed, error code in message
    MD5_MISMATCH(1003), // MD5 validation failed
}
```

The `mergedDefaultFunction` is a preset callback for handling results. In case of failure, it logs the error and shows a Toast; on success, it installs the new APK.

```Kotlin
// Default action after merge completion
fun mergedDefaultFunction(
    result : DiffResult,
    context: Context,
    authority : String = ".provider",
) 
```

#### Cache Cleanup

The `DiffUpdate` class provides a static method `clean` to manually clean the working directory:

```Kotlin
DiffUpdate.clean(context)
```

Even if developers don't manually clean, the `merge` function will clean the working directory first. If the merge fails, the working directory will be cleaned; after a successful merge, all files except the new APK will be cleaned.

The `clean` function only clears the working directory. Since the patch package is passed from external sources, it won't be cleaned by the library. Developers can delete it manually using `delete()` after a successful merge.

#### Example Usage

Here's an example of using the library with the file picker in Compose:

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

### Install APK

Ensure proper `FileProvider` configuration. You can pass either a `Uri` or a `File` (ensure storage permissions are granted if the file is in a public directory) to install the APK. It is recommended to pass the `Uri` (returned after download completes).

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

### Download File

The library also provides an asynchronous, `Flow`-based file download function using `DownloadManager` for downloading patch or installation files.

```Kotlin
object DownloadUtils {
    // Get file size in bytes
    suspend fun getFileSize(
        url: String,
        timeOutTime : Int = 5000
    ): Long?

    // Initialize, check file size, and whether the file has been downloaded (optional MD5 check)
    fun initDownloadFileStatus(
        fileName: String,
        destDir : File? = null,
        fileMd5 : String?,
        fileSize : Long?,
    ) : DownloadResult

    /**
     * @param context Context
     * @param url Download URL
     * @param fileName File name, including extension
     * @param fileMd5 Expected MD5, used to check if the file is already downloaded or if the downloaded file matches the expected MD5
     * @param destDir Destination directory, null means it will be downloaded to the public `Download` directory (requires storage permission)
     * @param delayTimesLong The delay time for downloading progress update, in milliseconds
     * @param requestBuilder Custom downloader builder
     * @param customDownloadId Custom download ID, null means it will be assigned by the system, and the download process will return the ID
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

The `DownloadResult` provides various download states, including preparation, download progress, success, and failure.

```Kotlin
sealed class DownloadResult {
    // The file already exists in the download directory, return File directly
    data class Downloaded(val file : File) : DownloadResult()
    // Downloading, progress ranges from 0 to 100, updated every `delayTimesLong` milliseconds
    data class Progress(val downloadId: Long, val progress: Int) : DownloadResult()
    // Download completed, the File or Uri can be processed, for example, Uri can be used to install the APK, the library provides this method by default, `InstallUtils.installApk()`
    data class Success(val downloadId: Long, val file: File, val uri: Uri, val checked : Boolean) : DownloadResult()
    // Download failed, logs and gives possible reasons
    data class Failed(val downloadId: Long, val reason: String?) : DownloadResult()
    // Preparation state, download not started, you can notify the user of the file size (optional)
    data class Prepare(val fileSize : Long? = null) : DownloadResult()
}
```

### Complete Example of Usage

Here’s an example of downloading and using the library in a `ViewModel`:

```Kotlin
class UpdateViewModel() : ViewModel() {
    private val _downloadState = MutableStateFlow<DownloadResult>(DownloadResult.Prepare)
    val downloadState: StateFlow<DownloadResult> = _downloadState

    private var downloadJob: Job? = null

    fun startDownload(url: String, filename: String, context: Context) {
        // Prevent multiple downloads
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
            // Preparation phase
            LargeButton(
                onClick = {
                    viewModel.startDownload("download_link", "filename", context)
                },
                text = "Download(${downloadState.fileSize}MB)",
            )
        }
        is DownloadResult.Downloaded -> {
            // Check if the file is downloaded
            LargeButton(
                onClick = {
                    scope.launch {
                        loadingPatch = true
                        // Merge and install
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
            // Update progress
            Text("${(downloadState as DownloadResult.Progress).progress}%")
        }
        is DownloadResult.Success -> {
            LargeButton(
                onClick = {
                    scope.launch {
                        loadingPatch = true
                        // Merge and install
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

### Declaration

Although the library has error handling and has been tested on different SDK versions, some issues may still arise due to limited testing devices and development experience.

### Compatibility (Based on Android Studio Emulator)

| API | Download File | Install APK  | Merge Incremental Package |
|-----|---------------|--------------|---------------------------|
| 24  | √             | √            | To be tested              |
| 25  | To be tested  | To be tested | To be tested              |
| 26  | √             | √            | To be tested              |
| 27  | To be tested  | To be tested | To be tested              |
| 28  | To be tested  | To be tested | To be tested              |
| 29  | √             | √            | To be tested              |
| 30  | To be tested  | To be tested | To be tested              |
| 31  | √             | √            | To be tested              |
| 32  | To be tested  | To be tested | To be tested              |
| 33  | To be tested  | To be tested | To be tested              |
| 34  | √             | √            | √                         |
| 35  | √             | √            | √                         |
| 36  | To be tested  | To be tested | To be tested              |

### Customization

If you need to customize, developers can import the **core** module, which only contains the Native layer, and make their own adjustments.

### Open Source Acknowledgements

* Bsdiff
* HPatchDiff

### Old Version
```Kotlin
implementation("com.github.Chiu-xaH:Bsdiff-Android-Library:1.0.3")
```
