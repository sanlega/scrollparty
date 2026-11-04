package com.santilega.scrollparty.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape

private const val BULBS = 13

/** Carcasa amarilla con labio, luces opcionales y ventana blanca. */
@Composable
fun Machine(modifier: Modifier = Modifier, lights: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Column(
        modifier.padding(bottom = 8.dp).drawBehind {
            drawRoundRect(SPColor.yellowDeep, Offset(0f, 8.dp.toPx()), Size(size.width, size.height), CornerRadius(30.dp.toPx()))
        }.background(SPColor.yellow, SPShape.r3).padding(12.dp),
    ) {
        if (lights) Bulbs()
        Box(
            Modifier.fillMaxWidth().clip(SPShape.r2).background(SPColor.white).drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(listOf(Color(0x33000000), Color.Transparent), endY = 10.dp.toPx()))
            },
            content = content,
        )
    }
}

@Composable
private fun Bulbs() {
    val reduce = LocalReduceMotion.current
    val blink = rememberInfiniteTransition(label = "bulbs").animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 900; 1f at 0; .35f at 450; 1f at 900 }, RepeatMode.Restart),
        label = "blink",
    )
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 9.dp).clearAndSetSemantics {}, Arrangement.SpaceBetween) {
        repeat(BULBS) { i ->
            val even = i % 2 == 0
            // Las pares y las impares alternan: una brilla cuando la otra se apaga.
            val alpha = if (reduce) 1f else if (even) blink.value else 1.35f - blink.value
            Box(Modifier.size(10.dp).alpha(alpha.coerceIn(.35f, 1f)).background(if (even) SPColor.coral else SPColor.blue, CircleShape))
        }
    }
}
