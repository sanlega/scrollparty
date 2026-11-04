package com.santilega.scrollparty.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.SPColor
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val RAYS = 15

/** Rayos vectoriales sobre blue, o night liso donde el vídeo es protagonista. */
@Composable
fun SunBackground(night: Boolean, modifier: Modifier = Modifier) {
    if (night) {
        Box(modifier.fillMaxSize().background(SPColor.night))
        return
    }
    val reduce = LocalReduceMotion.current
    val spin = rememberInfiniteTransition(label = "sun").animateFloat(
        0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "spin",
    )
    Canvas(modifier.fillMaxSize().background(SPColor.blue)) {
        val center = Offset(size.width / 2, size.height * 0.38f)
        val radius = max(size.width, size.height) * 1.3f
        rotate(if (reduce) 0f else spin.value, center) {
            val step = 360f / RAYS
            for (i in 0 until RAYS) {
                val a0 = Math.toRadians((i * step).toDouble())
                val a1 = Math.toRadians((i * step + step / 2).toDouble())
                val ray = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + radius * cos(a0).toFloat(), center.y + radius * sin(a0).toFloat())
                    lineTo(center.x + radius * cos(a1).toFloat(), center.y + radius * sin(a1).toFloat())
                    close()
                }
                drawPath(ray, SPColor.blueDeep)
            }
        }
    }
}
