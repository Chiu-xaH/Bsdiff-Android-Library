# Bsdiff-Lib    [![](https://jitpack.io/v/Chiu-xaH/Bsdiff-Lib.svg)](https://jitpack.io/#Chiu-xaH/Bsdiff-Lib)

适用于Android的库，集成了增量更新功能，需要开发者将生成的补丁包，推送给用户下载，下载到 内部存储/Download文件夹 后，调用mergePatchApk即可完成合并及其安装

## [增量包生成工具 Win端](https://github.com/Chiu-xaH/Bsdiff-Tool)

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
implementation("com.github.Chiu-xaH:Bsdiff-Lib:版本")
```
## 使用
这是一个示例代码，当点击按钮后即可开始合并补丁并跳转安装，**提前注意存储权限的获取**
```Kotlin
// APP从云端获取列表，然后对比自己当前版本若符合条件则取出其Patch，传给UI
val patchItem = getPatchVersions().find { item ->
    currentVersion == item.oldVersion
}
// 若有增量更新，则显示此UI
if (patchItem != null) {
    PatchUpdateUI(patchItem)
}

@Composable
fun PatchUpdateUI(patch: Patch) {
    // 下载界面
    Button(onClick = {
        // 从云端下载对应补丁包,这里自己搞DownloadManager即可
        // parsePatch是内置的函数，可以转换Patch为源文件名，与工具生成的名字一致
        downloadFile(fileName = parsePatch(patch))
    }) {
        Text("下载更新")
    }
    // ...
    
    // 下载完成
    var loading by remember { mutableStateOf(false) }
    // 当APP已经将补丁包下载到 内部存储/Download 文件夹完成后
    if (loading) {
        // 合并补丁中
        LoadingUI()
    } else {
        // 展示按钮
        Button(onClick = {
            checkStroagePermission()
            // patch是此依赖定义的数据类，
            BsdiffUpdate.mergePatchApk(context,patch) { loading = it }
        }) {
            Text("安装更新")
        }
    }
}
```
## 内容
BsdiffUpdate单例类中开放了三个函数，分别是
```Kotlin
// 安装合成好的APK
fun installNewApk(context: Context,authority : String = ".provider")
// 清除Android/data目录下的缓存APK
fun deleteCache(context: Context) : Boolean
// 判断 内部存储/Download 是否存在某文件，存在则返回其路径
fun isExistFile(fileName : String) : String?
// 合并并安装 patchFileName为补丁文件名，直接以工具生成的补丁文件名 旧versionName_to_新versionName.patch
fun mergePatchApk(context: Context, 
                  patch : Patch,
                  onSuccess : () -> Unit = { installNewApk(context) },
                  onLoad : (Boolean) -> Unit
                  ) : Boolean
```
mergePatchApk已经封装了 合并->安装->删除 操作，在不出错的前提下，可以直接完成操作,直接调用即可

顶层函数有一个数据类及其对应的两个转换函数，分别为
```Kotlin
data class Patch(val oldVersion : String,val newVersion : String)
// 将文件名转换为Patch，即解析工具生成的补丁包中的新旧版本信息
fun parsePatchFile(fileName : String) : Patch?
// 将Patch解析回fileName
fun parsePatch(patch : Patch) : String
```

## 说明
随便写写的，整体也没啥代码，就是方便自己用哈，只要你补丁包用我的工具生成(保证命名可以被APP解析)，APP通过自带的DownloadManager下载补丁包，自动下载到 内部存储/Download，就可以直接调用mergePatchApk，省的自己再写JNI，再封装，当然你也可以引入我的依赖，然后再定制
## 如何让APP识别是否可以用这个补丁包
我在工具和依赖都统一规定了补丁包文件名为：旧versionName_to_新versionName.patch，例如4.12_to_4.13.patch文件，用于v4.12用户升级到v4.13；当旧版本号与用户使用APP版本号相同时，即可下载补丁包，否则不显示增量更新



