package com.xah.bsdiff.networktest.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xah.bsdiff.networktest.logic.model.PatchAlgo
import com.xah.bsdiff.networktest.logic.util.AppVersion
import com.xah.bsdiff.networktest.ui.util.UiState
import com.xah.bsdiff.networktest.viewmodel.NetworkViewModel
import com.xah.patch.diff.DiffUpdate
import com.xah.patch.diff.model.DiffContent
import com.xah.patch.diff.model.DiffType
import com.xah.shared.download.downloadFile
import com.xah.shared.download.model.DownloadResult
import com.xah.shared.util.InstallUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UpdateViewModel() : ViewModel() {
    private val _downloadState = MutableStateFlow<DownloadResult>(
        DownloadResult.Prepare()
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
        _downloadState.value = DownloadResult.Prepare()
    }
}

@Composable
fun App(
    networkVm : NetworkViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val refreshNetwork : suspend () -> Unit = {
        networkVm.loginResult.clear()
        networkVm.getUpgrade()
    }
    val uiState by networkVm.loginResult.state.collectAsState()
    val downloadState by updateViewModel.downloadState.collectAsState()
    var md5 by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when(uiState) {
            is UiState.Error<*> -> {
                val error = uiState as UiState.Error
                Text(
                    "${error.exception?.message}(代号${error.code})",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Success<*> -> {
                val data = (uiState as UiState.Success).data
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ){
                    Text(data.msg)
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                        if(data.code == 200 && downloadState is DownloadResult.Prepare) {
                            val bean = data.data
                            // 有更新
                            md5 = bean.urlFileMd5
                            val newVersion = "${bean.versionName} (${bean.versionCode})"
                            if(bean.patchAlgo == PatchAlgo.DIFF.code) {
                                val downloadUrl = bean.patchUrlPath
                                val downloadSize = bean.patchUrlFileSize
                                Button(
                                    onClick = {
                                        scope.launch {
                                            updateViewModel.startDownload(
                                                downloadUrl,
                                                context.packageName + "_" + bean.versionName + ".patch",
                                                context
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("增量更新至${newVersion}(${downloadSize})")
                                }
                            } else {
                                val downloadUrl = bean.urlPath
                                val downloadSize = bean.urlFileSize
                                Button(
                                    onClick = {
                                        scope.launch {
                                            updateViewModel.startDownload(
                                                downloadUrl,
                                                context.packageName + "_" + bean.versionName + ".apk",
                                                context
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(.5f)
                                ) {
                                    Text("更新(${downloadSize})")
                                }
                            }
                        }
                    }
                }

            }
            is UiState.Loading -> {
                Text(
                    "正在检查",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Prepare -> {
                Text(
                    "版本 ${AppVersion.getVersionName()} (${AppVersion.getVersionCode()})",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }


        when(downloadState) {
            is DownloadResult.Prepare -> {
                Button(
                    onClick = {
                        scope.launch {
                            refreshNetwork()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Text("检查更新")
                }
            }
            is DownloadResult.Failed -> {
                Button(
                    onClick = {
                        scope.launch {
                            updateViewModel.reset()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Text("下载失败(重试)")
                }
            }
            is DownloadResult.Progress -> {
                val progress = (downloadState as DownloadResult.Progress).progress
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Text("${progress}%")
                }
            }
            is DownloadResult.Success -> {
                val state = (downloadState as DownloadResult.Success)
                val file = state.file
                var loading by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        scope.launch {
                            if(file.name.endsWith(".apk")) {
                                InstallUtils.installApk(state.uri, context)
                            } else {
                                loading = true
                                DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback(
                                    diffContent = DiffContent(
                                        diffFile = file,
                                        targetFileMd5 = md5
                                    ),
                                    context = context
                                )
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Text(if(loading)"正在合并" else "安装")
                }
            }
            is DownloadResult.Downloaded -> {
                val file = (downloadState as DownloadResult.Downloaded).file
                var loading by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        scope.launch {
                            if(file.name.endsWith(".apk")) {
                                InstallUtils.installApk(file, context)
                            } else {
                                loading = true
                                DiffUpdate(DiffType.H_DIFF_PATCH).mergeCallback(
                                    diffContent = DiffContent(
                                        diffFile = file,
                                        targetFileMd5 = md5
                                    ),
                                    context = context
                                )
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Text(if(loading)"正在合并" else "安装")
                }
            }
        }
    }
}