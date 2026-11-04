package com.santilega.scrollparty.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.santilega.scrollparty.Member
import com.santilega.scrollparty.R
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPType
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ITEM = 72.dp
private val HEIGHT = 310.dp

/**
 * Rueda de sospechosos: scroll con snap, la barra coral marca la opción central. Tocar una opción la centra y la selecciona.
 * [onSelect] recibe la opción central al asentarse el scroll.
 */
@Composable
fun SuspectWheel(members: List<Member>, initial: Int, onSelect: (Member) -> Unit, modifier: Modifier = Modifier) {
    val state = rememberLazyListState(initial.coerceIn(0, (members.size - 1).coerceAtLeast(0)))
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val reduce = LocalReduceMotion.current
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index ?: 0
        }
    }
    // Mientras se anima hacia una opción tocada no se pisa la selección con las intermedias.
    var target by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(centered, target) {
        val t = target
        if (t == null || t == centered) members.getOrNull(centered)?.let(onSelect)
    }
    LaunchedEffect(centered) { if (state.isScrollInProgress) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    Box(modifier.fillMaxWidth().height(HEIGHT)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(vertical = (HEIGHT - ITEM) / 2),
            flingBehavior = rememberSnapFlingBehavior(state),
        ) {
            itemsIndexed(members, key = { _, m -> m.id }) { index, m ->
                val isCentered = index == centered
                val scale = if (isCentered && !reduce) 1.04f else 1f
                Row(
                    Modifier.fillMaxWidth().height(ITEM)
                        .clickable(role = Role.RadioButton) {
                            target = index
                            onSelect(m)
                            scope.launch {
                                try { state.animateScrollToItem(index) } finally { target = null }
                            }
                        }
                        .semantics { selected = isCentered }
                        .padding(horizontal = 16.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PlayerFace(m.name, m.color, m.photo, Modifier.size(56.dp))
                    Text(
                        m.name, style = SPType.heading.copy(fontSize = 23.sp),
                        color = if (isCentered) SPColor.ink else SPColor.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Fundidos arriba y abajo y barra coral central.
        Box(Modifier.fillMaxWidth().height(46.dp).background(Brush.verticalGradient(listOf(Color.White, Color.Transparent))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(46.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.White))))
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth().height(ITEM + 4.dp).drawBehind {
                val line = Size(size.width, 4.dp.toPx())
                drawRect(SPColor.coral, Offset.Zero, line)
                drawRect(SPColor.coral, Offset(0f, size.height - line.height), line)
            },
        )
    }
}
