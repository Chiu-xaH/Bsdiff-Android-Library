# Bsdiff-Lib    [![](https://jitpack.io/v/Chiu-xaH/Bsdiff-Lib.svg)](https://jitpack.io/#Chiu-xaH/Bsdiff-Lib)

适用于Android的库，集成了增量更新



![图片](/img/a.png)


## 食用方法
在settings.gradle添加
```Groovy
maven { url 'https://jitpack.io' }
```

添加依赖，版本以Tag为准
```Groovy
implementation("com.github.Chiu-xaH:Bsdiff-Lib:XX")
```

BsdiffUpdate单例类中开放了三个函数，分别是
```Kotlin
// 安装合成好的APK
fun installNewApk(context: Context,authority : String = ".provider") { ... }
// 清除Android/data目录下的缓存APK
fun deleteCache(context: Context) : Boolean { ... }
// 合并并安装 patchFileName为补丁文件名，可以直接以BsdiffTool生成的补丁文件名 旧版本号_to_新版本号.patch
fun mergePatchApk(context: Context, 
                  patchFileName : String, 
                  onLoad : (Boolean) -> Unit, 
                  onSuccess : () -> Unit = { installNewApk(context) }) : Boolean { ... }
```
mergePatchApk已经封装了 合并->安装->删除 操作，在不出错的前提下，可以直接完成操作,直接调用即可

## [增量包生成工具 Win端](https://github.com/Chiu-xaH/Bsdiff-Tool)

## 说明
着急写出来的，建议自己将BsdiffUpdate单例类重新定制，更符合胃口，毕竟代码这么少比较好写😂

## 如何让APP识别是否可以用这个补丁包
我自己统一规定了补丁包文件名称都为：旧版本号_to_新版本号.patch

当 旧版本号与用户使用APP相同时，即可使用补丁包，否则不显示增量更新的入口 ,例如
```Kotlin
data class Patch(val oldVersion : String,val newVersion : String)

fun getPatchVersions(resources : String) : List<Patch> {
    val e = ".patch"
    // 假设这是一批下载资源的列表
    return resources.mapNotNull { element ->
        val text = element.text().trim()
        if (text.endsWith(e)) {
            // 找到以.patch结尾，代表补丁
            val str = text.substringBefore(e)
            // 取旧版本号与新版本号
            val old = str.substringBefore("_to_")
            val new = str.substringAfter("_to_")
            Patch(old,new)
        } else {
            null
        }
    }
}
```



