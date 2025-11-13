package com.xah.bsdiffs.test

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xah.bsdiffs.patch.DiffWithMetaUpdate
import com.xah.bsdiffs.test.ui.theme.BsdiffLibTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BsdiffLibTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
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
                                            DiffWithMetaUpdate().mergeCallback(it, context)
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
            }
        }
    }
}

fun uriToFile(context: Context, uri: Uri): File? {
    val contentResolver = context.contentResolver
    val fileName = queryName(contentResolver, uri)
    val tempFile = File(context.cacheDir, fileName ?: "temp_patch_${System.currentTimeMillis()}.patch")

    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun queryName(resolver: ContentResolver, uri: Uri): String? {
    val returnCursor = resolver.query(uri, null, null, null, null)
    returnCursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && it.moveToFirst()) {
            return it.getString(nameIndex)
        }
    }
    return null
}
