package com.santilega.scrollparty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPType

/** Rectángulo con una muesca semicircular a cada lado. */
private object NotchedShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { 8.dp.toPx() }
        val body = Path().apply { addRect(Rect(Offset.Zero, size)) }
        val holes = Path().apply {
            addOval(Rect(Offset(-r, size.height / 2 - r), Size(2 * r, 2 * r)))
            addOval(Rect(Offset(size.width - r, size.height / 2 - r), Size(2 * r, 2 * r)))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, holes))
    }
}

/** Tique con muescas laterales: mint para aciertos, blush para «Fooled». Texto siempre ink. */
@Composable
fun Ticket(text: String, modifier: Modifier = Modifier, fooled: Boolean = false, small: Boolean = false) {
    Text(
        text,
        modifier.clip(NotchedShape).background(if (fooled) SPColor.blush else SPColor.mint)
            .padding(horizontal = if (small) 15.dp else 20.dp, vertical = if (small) 7.dp else 9.dp),
        style = SPType.button.copy(fontSize = if (small) 14.sp else 18.sp, lineHeight = if (small) 16.sp else 20.sp),
        color = SPColor.ink,
        textAlign = TextAlign.Center,
    )
}
