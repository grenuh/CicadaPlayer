package com.example.cicadaplayer.util

import android.net.Uri
import android.provider.DocumentsContract

/**
 * Extracts a human-readable display name from a tree URI string.
 * e.g. "content://...documents/tree/primary%3AMusic" -> "Internal/Music"
 */
fun treeUriToDisplayName(uriString: String): String {
    val uri = Uri.parse(uriString)
    val docId = try {
        DocumentsContract.getTreeDocumentId(uri)
    } catch (e: Exception) {
        return uriString
    }
    val parts = docId.split(":")
    return if (parts.size == 2) {
        val storage = if (parts[0].equals("primary", ignoreCase = true)) "Internal" else parts[0]
        val path = parts[1].ifEmpty { "Root" }
        "$storage/$path"
    } else {
        uriString
    }
}
