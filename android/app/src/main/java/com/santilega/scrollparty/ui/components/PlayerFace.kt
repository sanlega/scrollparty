package com.santilega.scrollparty.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.santilega.scrollparty.decodePhoto
import com.santilega.scrollparty.initials
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPType
import com.santilega.scrollparty.ui.theme.playerColor

private val Gray = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
private const val DOTS = 25

/** Selfie en grises por el color del jugador con trama de puntos; sin foto, iniciales en ink. */
@Composable
fun PlayerFace(name: String, color: Int, photo: String?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val image = remember(photo) { photo?.let { decodePhoto(it) } }
    val tint = playerColor(color)
    BoxWithConstraints(
        modifier.clip(RoundedCornerShape(14.dp)).then(
            if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier.clearAndSetSemantics {},
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(tint)
            if (image != null) {
                drawImage(image, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = Gray)
                drawRect(tint, blendMode = BlendMode.Multiply)
            }
            val cell = size.width / DOTS
            val radius = maxOf(cell * 0.2f, 0.6f)
            for (i in 0 until DOTS) for (j in 0 until DOTS) {
                drawCircle(SPColor.ink.copy(alpha = .28f), radius, Offset((i + .5f) * cell, (j + .5f) * cell))
            }
        }
        if (image == null) {
            Text(initials(name), style = SPType.heading, color = SPColor.ink, fontSize = with(LocalDensity.current) { (maxWidth * 0.38f).toSp() })
        }
    }
}
