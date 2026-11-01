package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.BoardPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ExportShareHelper {

    suspend fun exportPageToPng(
        context: Context,
        page: BoardPage,
        projectTitle: String,
        pageIndex: Int,
        canvasWidth: Int = 1080,
        canvasHeight: Int = 1920
    ): File? = withContext(Dispatchers.IO) {
        try {
            val w = canvasWidth.coerceAtLeast(720)
            val h = canvasHeight.coerceAtLeast(1080)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            CanvasRenderEngine.renderPageToAndroidCanvas(
                canvas = canvas,
                width = w.toFloat(),
                height = h.toFloat(),
                page = page
            )
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = projectTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "SmartBoard" }
            val outFile = File(exportDir, "${safeName}_Page_${pageIndex + 1}.png")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    suspend fun exportAllPagesToPdf(
        context: Context,
        pages: List<BoardPage>,
        projectTitle: String,
        canvasWidth: Int = 1080,
        canvasHeight: Int = 1920
    ): File? = withContext(Dispatchers.IO) {
        try {
            val w = canvasWidth.coerceAtLeast(720)
            val h = canvasHeight.coerceAtLeast(1080)
            val pdfDocument = PdfDocument()

            pages.forEachIndexed { index, page ->
                val pageInfo = PdfDocument.PageInfo.Builder(w, h, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                // Render through a Bitmap so offscreen PorterDuff.CLEAR layers for Pixel Eraser work identically
                val pageBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val bmpCanvas = Canvas(pageBitmap)
                CanvasRenderEngine.renderPageToAndroidCanvas(
                    canvas = bmpCanvas,
                    width = w.toFloat(),
                    height = h.toFloat(),
                    page = page
                )
                pdfPage.canvas.drawBitmap(pageBitmap, 0f, 0f, null)
                pdfDocument.finishPage(pdfPage)
                pageBitmap.recycle()
            }

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = projectTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "SmartBoard" }
            val outFile = File(exportDir, "${safeName}_All_Pages.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            outFile
        } catch (e: Exception) {
            null
        }
    }

    fun shareExportedFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, file.name).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
