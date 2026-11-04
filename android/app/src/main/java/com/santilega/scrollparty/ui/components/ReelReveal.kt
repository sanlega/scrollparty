package com.santilega.scrollparty.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.santilega.scrollparty.Member
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.Motion
import com.santilega.scrollparty.ui.theme.SPColor
import kotlin.random.Random

/**
 * Rodillo único con las caras de los jugadores que acaba en [author]. Con movimiento reducido: fundido de 150 ms.
 * Llama a [onLanded] al parar; la háptica se mantiene siempre.
 */
@Composable
fun ReelReveal(pool: List<Member>, author: Member, onLanded: () -> Unit, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    val strip = remember(pool, author) { pool + pool + author }
    val progress = remember { Animatable(0f) }
    val alpha = remember { Animatable(if (reduce) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (reduce) {
            progress.snapTo(1f)
            alpha.animateTo(1f, tween(Motion.FADE_MS))
        } else {
            progress.animateTo(1f, tween(Motion.REEL_MS, easing = Motion.reelEasing))
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onLanded()
    }
    Machine(modifier, lights = true) {
        BoxWithConstraints(Modifier.fillMaxWidth().clipToBounds().clearAndSetSemantics {}) {
            val cell = maxWidth
            Column(
                Modifier.height(cell).wrapContentHeight(Alignment.Top, unbounded = true).graphicsLayer {
                    this.alpha = alpha.value
                    translationY = -progress.value * (strip.size - 1) * cell.toPx()
                },
            ) {
                strip.forEach { PlayerFace(it.name, it.color, it.photo, Modifier.fillMaxWidth().height(cell).padding(8.dp)) }
            }
        }
    }
}

private class Piece(val x: Float, val color: Color, val delay: Float, val length: Float, val spin: Float)

/** Confeti que cae una vez; no se usa con movimiento reducido. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val colors = listOf(SPColor.mint, SPColor.yellow, SPColor.coral, SPColor.white)
    val pieces = remember { List(24) { Piece(Random.nextFloat(), colors[it % 4], Random.nextFloat() * .25f, .5f + Random.nextFloat() * .25f, 360f + Random.nextFloat() * 360f) } }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(2400, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize().clearAndSetSemantics {}) {
        val w = 22.dp.toPx()
        val h = 11.dp.toPx()
        pieces.forEach { p ->
            val t = (progress.value - p.delay) / p.length
            if (t in 0f..1f) {
                val x = p.x * size.width
                val y = -h + t * (size.height + 2 * h)
                rotate(p.spin * t, Offset(x + w / 2, y + h / 2)) { drawRect(p.color, Offset(x, y), Size(w, h)) }
            }
        }
    }
}
