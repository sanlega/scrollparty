package com.santilega.scrollparty

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhotoTest {
    /** El peor caso para JPEG es el ruido: aun así el base64 debe caber en las reglas (40 000). */
    @Test fun laFotoSiempreCabeEnLasReglas() {
        val noise = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888)
        val random = Random(1)
        for (y in 0 until noise.height step 2) for (x in 0 until noise.width step 2) {
            noise.setPixel(x, y, Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
        }
        val base64 = squareJpegBase64(noise)
        assertTrue("${base64.length} caracteres", base64.length <= MAX_PHOTO_CHARS)
        val decoded = decodePhoto(base64)
        assertNotNull(decoded)
        assertEquals("cuadrada", decoded!!.width, decoded.height)
        assertTrue(decoded.width <= PHOTO_SIZE)
    }
}
