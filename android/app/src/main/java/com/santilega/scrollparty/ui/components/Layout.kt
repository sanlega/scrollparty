package com.santilega.scrollparty.ui.components

import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.Motion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/**
 * Pantalla con fondo, margen de 24 y safe area. El contenido hace scroll (fontScale 2 sin solapes)
 * y [bottom] queda fijo abajo.
 */
@Composable
fun ScreenScaffold(
    night: Boolean = false,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        SunBackground(night)
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(start = SPSpace.margin, end = SPSpace.margin, top = SPSpace.s2, bottom = SPSpace.s3)) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
            if (bottom != null) Column(Modifier.padding(top = SPSpace.s2), content = bottom)
        }
        overlay()
    }
}

/** Campo blanco con etiqueta encima; etiqueta y texto se leen juntos. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color = SPColor.soft,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    tag: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = SPType.label, color = labelColor, modifier = Modifier.clearAndSetSemantics {})
        BasicTextField(
            value, onValueChange,
            Modifier.testTag(tag.orEmpty()).semantics { contentDescription = label }.fillMaxWidth().padding(top = 6.dp).heightIn(min = 58.dp).background(SPColor.white, SPShape.r1),
            textStyle = SPType.body.copy(fontSize = 20.sp, color = SPColor.ink),
            cursorBrush = SolidColor(SPColor.ink),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            decorationBox = { inner -> Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), contentAlignment = Alignment.CenterStart) { inner() } },
        )
    }
}

/** Entrada con rebote (spring); con movimiento reducido, fundido de 150 ms. */
@Composable
fun Modifier.pop(): Modifier {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (reduce) progress.animateTo(1f, tween(Motion.FADE_MS)) else progress.animateTo(1f, Motion.spring) }
    return graphicsLayer {
        val p = progress.value
        alpha = if (reduce) p else p.coerceIn(0f, 1f)
        if (!reduce) {
            val s = .3f + .7f * p
            scaleX = s; scaleY = s; rotationZ = -8f * (1f - p)
        }
    }
}

/** Entrada que sube 14 dp (snap); con movimiento reducido, fundido de 150 ms. */
@Composable
fun Modifier.rise(): Modifier {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (reduce) progress.animateTo(1f, tween(Motion.FADE_MS)) else progress.animateTo(1f, Motion.snap) }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        if (!reduce) translationY = (1f - progress.value) * 14.dp.toPx()
    }
}

/** Texto secundario pulsable de 48 dp de alto mínimo. */
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = SPSpace.minTouch).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SPType.body.copy(fontSize = 16.sp), color = SPColor.white, textAlign = TextAlign.Center)
    }
}

/** Fondo de marcador de vídeo: no se descargan vídeos, solo se guardan enlaces. */
fun Modifier.videoBackground(): Modifier = background(
    Brush.verticalGradient(0f to Color(0xFFFF6A5F), .55f to Color(0xFFA331E0), 1f to Color(0xFF20124A)),
)

@Composable
fun PlayGlyph(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, color: Color = SPColor.white) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * .36f, w * .25f); lineTo(w * .78f, w * .5f); lineTo(w * .36f, w * .75f); close()
        }
        drawPath(path, color)
    }
}
