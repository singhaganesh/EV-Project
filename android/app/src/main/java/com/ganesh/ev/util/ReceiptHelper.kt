package com.ganesh.ev.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ganesh.ev.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads the payment-receipt PDF (D2) and shares/opens it.
 *
 * The receipt is served as a binary PDF from the backend; we stream it to the
 * app cache and expose it through a [FileProvider] so it can be shared without
 * any storage permission.
 */
object ReceiptHelper {

    /** Downloads the receipt for [sessionId] into the cache; returns the file or null on failure. */
    suspend fun download(context: Context, sessionId: Long): File? = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.downloadReceipt(sessionId)
            val body = response.body()
            if (!response.isSuccessful || body == null) return@withContext null
            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(dir, "plugsy-receipt-$sessionId.pdf")
            body.byteStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    /** Opens the system share sheet for a downloaded receipt PDF. */
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share receipt"))
    }
}
