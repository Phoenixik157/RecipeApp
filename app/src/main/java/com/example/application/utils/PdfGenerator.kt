package com.example.application.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.application.data.local.entity.RecipeEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    // Та же ссылка, что используется в QR
    private const val BASE_URL = "https://phoenixik157.github.io/recipe-web/recipe.html?id="

    fun generateRecipePdf(context: Context, recipe: RecipeEntity, onResult: (Boolean, String) -> Unit) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = Color.BLACK
            }
            val textPaint = Paint().apply {
                textSize = 14f
                color = Color.DKGRAY
            }

            var y = 50f
            val margin = 40f

            // 1. Заголовок
            canvas.drawText(recipe.title, margin, y, titlePaint)
            y += 30f

            // 2. Время и сложность
            canvas.drawText("Время: ${recipe.cookingTimeMinutes} мин | Сложность: ${recipe.complexity}/3", margin, y, textPaint)
            y += 40f

            // 3. QR-код
            val qrSize = 200
            val qrBitmap = generateQr(recipe, qrSize)
            canvas.drawBitmap(qrBitmap, margin, y, Paint())

            // Текст рядом с QR
            val textX = margin + qrSize + 20f
            canvas.drawText("Отсканируйте,", textX, y + 40f, textPaint)
            canvas.drawText("чтобы открыть", textX, y + 60f, textPaint)
            canvas.drawText("в приложении", textX, y + 80f, textPaint)

            y += qrSize + 30f // Спускаемся чуть ниже QR-кода

            // --- ДОБАВЛЯЕМ ССЫЛКУ ---
            val linkPaint = Paint().apply {
                textSize = 12f
                color = Color.parseColor("#2563EB") // Синий цвет ссылки
                isUnderlineText = true // Подчеркиваем, чтобы было похоже на кликабельную ссылку
            }
            val linkText = "$BASE_URL${recipe.id}"
            canvas.drawText(linkText, margin, y, linkPaint)

            y += 40f // Отступ до инструкции
            // -------------------------

            // 4. Инструкция (с переносом строк)
            val subtitlePaint = Paint().apply {
                textSize = 18f
                isFakeBoldText = true
                color = Color.BLACK
            }
            canvas.drawText("Инструкция:", margin, y, subtitlePaint)
            y += 25f

            val instructions = recipe.instructions.split("\n")
            val wrappedTextPaint = Paint().apply {
                textSize = 12f
                color = Color.BLACK
            }

            for (line in instructions) {
                val words = line.split(" ")
                var currentLine = ""
                for (word in words) {
                    if (wrappedTextPaint.measureText("$currentLine $word") < 515f) {
                        currentLine += if (currentLine.isEmpty()) word else " $word"
                    } else {
                        canvas.drawText(currentLine, margin, y, wrappedTextPaint)
                        y += 20f
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, margin, y, wrappedTextPaint)
                    y += 20f
                }
                if (y > 800f) break
            }

            pdfDocument.finishPage(page)

            // 5. Сохранение файла
            val fileName = "Рецепт_${recipe.title.replace(" ", "_")}.pdf"
            val uri = savePdf(context, pdfDocument, fileName)
            pdfDocument.close()

            if (uri != null) {
                onResult(true, "PDF сохранен в Загрузки")
            } else {
                onResult(false, "Ошибка сохранения файла")
            }
        } catch (e: Exception) {
            onResult(false, "Ошибка: ${e.message}")
        }
    }

    private fun generateQr(recipe: RecipeEntity, size: Int): Bitmap {
        val link = "$BASE_URL${recipe.id}"
        val writer = QRCodeWriter()
        val matrix = writer.encode(link, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    private fun savePdf(context: Context, document: PdfDocument, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    document.writeTo(outputStream)
                }
            }
            uri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            Uri.fromFile(file)
        }
    }
}