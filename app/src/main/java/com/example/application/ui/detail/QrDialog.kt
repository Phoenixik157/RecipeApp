package com.example.application.ui.detail

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.application.R
import com.example.application.data.local.entity.RecipeEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.view.View

object QrDialog {

    private const val BASE_URL = "https://phoenixik157.github.io/recipe-web/recipe.html"

    fun show(context: Context, recipe: RecipeEntity) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_qr, null)

        val link = "$BASE_URL?id=${recipe.id}"

        // QR-код
        val qrBitmap = generateQr(link, 512)
        view.findViewById<ImageView>(R.id.qrImage).setImageBitmap(qrBitmap)

        // Информация
        view.findViewById<TextView>(R.id.recipeName).text = recipe.title
        view.findViewById<TextView>(R.id.recipeLink).text = link

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Закрытие
        view.findViewById<View>(R.id.closeBtn).setOnClickListener { dialog.dismiss() }

        // Копирование ссылки
        view.findViewById<View>(R.id.copyBtn).setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("recipe_link", link))
            Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
        }

        // Сохранение PDF
        view.findViewById<View>(R.id.savePdfBtn).setOnClickListener {
            Toast.makeText(context, "PDF-экспорт скоро появится", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    /** Генерирует Bitmap с QR-кодом для текста. */
    private fun generateQr(text: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}