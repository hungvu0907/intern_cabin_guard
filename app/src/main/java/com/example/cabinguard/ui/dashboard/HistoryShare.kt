package com.example.cabinguard.ui.dashboard

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.cabinguard.data.export.TelemetryCsvCache

/**
 * [EXP-03][EXP-04] FileProvider URI từ file cache, rồi mở share sheet (Zalo / Email / Drive).
 */
object HistoryShare {

    fun share(context: Context, csv: String) {
        val file = TelemetryCsvCache.write(context.cacheDir, csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CabinGuard — lịch sử cabin")
            clipData = ClipData.newRawUri("CabinGuard history", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Chia sẻ lịch sử CSV").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}
