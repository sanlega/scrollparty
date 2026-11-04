package com.santilega.scrollparty.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.Motion
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/** Primaria (amarilla) o alt (blanca), con labio que se hunde al pulsar. Desactivado: night2, sin bajar opacidad. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alt: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
    faceColor: Color? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val sink by animateDpAsState(if (pressed && enabled && !loading) 6.dp else 0.dp, tween(if (reduce) 0 else Motion.PRESS_MS), label = "press")
    val face = when { !enabled -> SPColor.night2; faceColor != null -> faceColor; alt -> SPColor.white; else -> SPColor.yellow }
    val lip = when { !enabled -> SPColor.night2; alt -> SPColor.whiteLip; else -> SPColor.yellowDeep }
    Column(
        modifier.fillMaxWidth().drawBehind {
            val l = SPSpace.lip.toPx()
            drawRoundRect(lip, Offset(0f, l), Size(size.width, size.height - l), CornerRadius(22.dp.toPx()))
        },
    ) {
        Box(
            Modifier.fillMaxWidth()
                .offset { IntOffset(0, sink.roundToPx()) }
                .heightIn(min = SPSpace.buttonHeight)
                .background(face, SPShape.r2)
                .then(if (!enabled) Modifier.border(BorderStroke(2.dp, SPColor.soft.copy(alpha = .33f)), SPShape.r2) else Modifier)
                .clickable(source, null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = text }
                .padding(horizontal = SPSpace.s2, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(28.dp), color = SPColor.ink, strokeWidth = 3.dp)
            else Text(text.uppercase(), style = SPType.button, color = if (enabled) SPColor.ink else SPColor.soft, textAlign = TextAlign.Center, maxLines = 2)
        }
        Spacer(Modifier.height(SPSpace.lip))
    }
}
