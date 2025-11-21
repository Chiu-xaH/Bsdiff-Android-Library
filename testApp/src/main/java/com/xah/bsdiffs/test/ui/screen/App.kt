package com.xah.bsdiffs.test.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xah.bsdiffs.test.util.AppVersion
import com.xah.bsdiffs.test.util.queryName
import com.xah.bsdiffs.test.util.uriToFile
import com.xah.patch.diff.DiffUpdate
import com.xah.patch.diff.model.DiffType
import kotlinx.coroutines.launch

@Composable
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            "版本 ${AppVersion.getVersionName()} (${AppVersion.getVersionCode()})",
            modifier = Modifier.align(Alignment.Center)
        )

        Button(
            onClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            Text("选择补丁包")
        }
    }
}