package com.santilega.scrollparty.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.R
import com.santilega.scrollparty.REACTIONS
import com.santilega.scrollparty.reactionChanges
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType
import kotlin.random.Random

fun reactionEmoji(key: String) = when (key) {
    "laugh" -> "😂"
    "skull" -> "💀"
    "fire" -> "🔥"
    else -> "😴"
}

@StringRes
private fun reactionLabel(key: String) = when (key) {
    "laugh" -> R.string.react_laugh
    "skull" -> R.string.react_skull
    "fire" -> R.string.react_fire
    else -> R.string.react_sleep
}

/** Cuatro reacciones en directo; la elegida se marca en mint y se anuncia como seleccionada. */
@Composable
fun ReactionBar(selected: String?, onReact: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        REACTIONS.forEach { key -> ReactionButton(key, key == selected, onReact) }
    }
}

@Composable
private fun RowScope.ReactionButton(key: String, selected: Boolean, onReact: (String) -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    val label = stringResource(reactionLabel(key))
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) { if (!reduce) scale.animateTo(if (pressed) 1.2f else 1f, tween(120)) }
    Box(
        Modifier.weight(1f).heightIn(min = 58.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .drawBehind { drawRoundRect(SPColor.whiteLip, Offset(0f, 4.dp.toPx()), size, CornerRadius(14.dp.toPx())) }
            .background(if (selected) SPColor.mint else SPColor.white, SPShape.r1)
            .clickable(source, null, role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onReact(key) }
            .semantics { contentDescription = label; this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(reactionEmoji(key), fontSize = 27.sp, modifier = Modifier.clearAndSetSemantics {})
    }
}

private class Fly(val id: Int, val emoji: String, val x: Float)

/**
 * La tele lanza cada reacción que aparece o cambia. Con movimiento reducido, un contador por tipo.
 * Ocupa todo el padre; no intercepta toques.
 */
@Composable
fun FlyingReactions(reactions: Map<String, String>, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    var previous by remember { mutableStateOf(reactions) }
    var nextId by remember { mutableIntStateOf(0) }
    val flying = remember { mutableStateListOf<Fly>() }
    LaunchedEffect(reactions) {
        if (!reduce) reactionChanges(previous, reactions).forEach { flying += Fly(nextId++, reactionEmoji(it), Random.nextFloat()) }
        previous = reactions
    }
    if (reduce) {
        Row(modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            REACTIONS.forEach { key ->
                val count = reactions.values.count { it == key }
                if (count > 0) Text(
                    "${reactionEmoji(key)} $count",
                    Modifier.background(SPColor.white, RoundedCornerShape(99.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    style = SPType.body.copy(fontSize = 16.sp), color = SPColor.ink,
                )
            }
        }
    } else {
        BoxWithConstraints(modifier.fillMaxSize().clearAndSetSemantics {}) {
            flying.forEach { fly -> key(fly.id) { FlyingEmoji(fly, maxWidth.value, maxHeight.value) { flying.remove(fly) } } }
        }
    }
}

@Composable
private fun FlyingEmoji(fly: Fly, width: Float, height: Float, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
        onDone()
    }
    Text(
        fly.emoji,
        Modifier
            .offset(x = ((width - 56f) * fly.x).dp, y = (height - 220f).dp)
            .graphicsLayer {
                val p = progress.value
                alpha = if (p < .15f) p / .15f else (1f - p) / .85f
                translationY = (16.dp.toPx()) - 316.dp.toPx() * p
                scaleX = .6f + .55f * minOf(p / .15f, 1f); scaleY = scaleX
                rotationZ = 16f * p
            },
        fontSize = 40.sp,
    )
}
