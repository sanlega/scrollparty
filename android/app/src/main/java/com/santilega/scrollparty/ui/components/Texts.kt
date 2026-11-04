package com.santilega.scrollparty.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.Motion
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape
import com.santilega.scrollparty.ui.theme.SPType

/** Titular Bowlby One en mayúsculas que se ajusta solo, sin saltos manuales. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DisplayTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = SPType.display,
    color: Color = SPColor.yellow,
    shadow: Boolean = true,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 3,
) {
    val max = style.fontSize
    BasicText(
        text.uppercase(),
        modifier,
        style = style.copy(
            color = color,
            textAlign = align,
            lineHeight = 1.04.em,
            shadow = if (shadow) Shadow(SPColor.blueDeep, Offset(0f, 4f), 0f) else null,
        ),
        maxLines = maxLines,
        autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = max, stepSize = 1.sp),
    )
}

/** Tarjeta con la pista anónima de la ronda. */
@Composable
fun ClueCard(label: String, clue: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(SPColor.white, SPShape.r1).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label.uppercase(), style = SPType.label, color = SPColor.inkSoft)
        Text("“$clue”", style = SPType.body, color = SPColor.ink)
    }
}

/** Aviso amarillo con labio; se anuncia solo a los lectores de pantalla. */
@Composable
fun Toast(title: String, modifier: Modifier = Modifier, detail: String? = null) {
    val reduce = LocalReduceMotion.current
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduce) enter.snapTo(1f) else enter.animateTo(1f, Motion.spring)
    }
    Column(
        modifier.fillMaxWidth()
            .graphicsLayer { alpha = enter.value.coerceIn(0f, 1f); translationY = (1f - enter.value) * 14.dp.toPx() }
            .drawBehind { drawRoundRect(SPColor.yellowDeep, Offset(0f, 7.dp.toPx()), size, CornerRadius(14.dp.toPx())) }
            .background(SPColor.yellow, SPShape.r1)
            .padding(horizontal = 18.dp, vertical = 15.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(title, style = SPType.body, color = SPColor.ink)
        if (detail != null) Text(detail, style = SPType.caption, color = SPColor.ink)
    }
}

/** Tarjeta de premio: cara del premiado, título y nombres. */
@Composable
fun PrizeCard(title: String, names: String, face: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.background(SPColor.white, SPShape.r1).padding(horizontal = 4.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title: $names" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        face()
        Spacer(Modifier.height(5.dp))
        Text(title, style = SPType.label.copy(letterSpacing = 0.sp), color = SPColor.ink, textAlign = TextAlign.Center)
        Text(names, style = SPType.caption, color = SPColor.inkSoft, textAlign = TextAlign.Center, maxLines = 2)
    }
}
