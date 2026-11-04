package com.santilega.scrollparty

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

const val PHOTO_SIZE = 256
const val MAX_PHOTO_CHARS = 40_000 // igual que firestore.rules

/** Recorta al centro en cuadrado, 256 px, JPEG; baja calidad (y como último recurso tamaño) hasta que el base64 cabe. */
fun squareJpegBase64(src: Bitmap): String {
    val side = minOf(src.width, src.height)
    val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    var size = PHOTO_SIZE
    while (true) {
        val scaled = Bitmap.createScaledBitmap(square, size, size, true)
        for (quality in 85 downTo 25 step 15) {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            if (base64.length <= MAX_PHOTO_CHARS) return base64
        }
        size = size * 4 / 5
    }
}

fun decodePhoto(base64: String): ImageBitmap? = runCatching {
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()

/** Lee una foto de la galería a un tamaño razonable (sin permisos: el selector del sistema da acceso al Uri). */
fun bitmapFromUri(context: Context, uri: Uri): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}.getOrNull()
