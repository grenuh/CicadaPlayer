package com.example.cicadaplayer.util

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

fun treeUriToFilePath(uri: Uri): String? {
    val docId = DocumentsContract.getTreeDocumentId(uri)
    val parts = docId.split(":")
    if (parts.size != 2) return null
    val storageType = parts[0]
    val relativePath = parts[1]
    val basePath = if (storageType.equals("primary", ignoreCase = true)) {
        Environment.getExternalStorageDirectory().path
    } else {
        "/storage/$storageType"
    }
    return "$basePath/$relativePath"
}
