package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileActionsHelper {

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun sharePdf(context: Context, file: File, displayTitle: String): Result<Unit> {
        return runCatching {
            if (!file.exists()) error("পিডিএফ ফাইলটি পাওয়া যায়নি।")
            val uri = getFileProviderUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, displayTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "পিডিএফ শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun shareImages(context: Context, files: List<File>): Result<Unit> {
        return runCatching {
            if (files.isEmpty()) error("শেয়ার করার জন্য কোনো ছবি নেই।")
            val uris = ArrayList(files.filter { it.exists() }.map { getFileProviderUri(context, it) })
            if (uris.isEmpty()) error("ছবিগুলো পাওয়া যায়নি।")
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(intent, "ছবি শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun printPdf(context: Context, file: File, jobName: String): Result<Unit> {
        return runCatching {
            if (!file.exists()) error("প্রিন্ট করার জন্য পিডিএফ ফাইলটি পাওয়া যায়নি।")
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: error("এই ডিভাইসে প্রিন্ট সেবা চালু করা যায়নি।")
            val adapter = LocalPdfPrintAdapter(file, jobName)
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileNameWithoutExt: String): Result<String> {
        return runCatching {
            val fileName = "${fileNameWithoutExt}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/SahajPdfMaker"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("গ্যালারিতে ছবি সংরক্ষণ করা যায়নি।")
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        error("ছবিটি সংরক্ষণ করতে সমস্যা হয়েছে।")
                    }
                } ?: error("ফাইল আউটপুট স্ট্রিম খোলা যায়নি।")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Pictures/SahajPdfMaker/$fileName"
            } else {
                val dir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "SahajPdfMaker"
                ).apply { mkdirs() }
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                outFile.absolutePath
            }
        }
    }

    fun copyUriToFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            destFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            destFile.exists() && destFile.length() > 0L
        } catch (_: Exception) {
            false
        }
    }

    fun copyFileToUri(context: Context, sourceFile: File, destUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun queryDisplayName(context: Context, uri: Uri): String {
        var result: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) result = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {
        }
        return (result ?: uri.lastPathSegment ?: BengaliFormatters.defaultPdfTitle())
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
    }

    fun availableInternalBytes(context: Context): Long {
        return try {
            val dir = context.filesDir
            val stat = StatFs(dir.absolutePath)
            when {
                stat.totalBytes > 0L -> stat.availableBytes
                dir.totalSpace > 0L -> dir.usableSpace
                else -> Long.MAX_VALUE
            }
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }
}

private class LocalPdfPrintAdapter(
    private val file: File,
    private val fileName: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("$fileName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback.onWriteCancelled()
                            return
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message ?: "প্রিন্ট করতে সমস্যা হয়েছে")
        }
    }
}
