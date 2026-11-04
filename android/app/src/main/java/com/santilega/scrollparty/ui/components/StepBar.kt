package com.santilega.scrollparty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.santilega.scrollparty.R
import com.santilega.scrollparty.ui.theme.SPColor

/** Barras de progreso del onboarding; [step] empieza en 0. */
@Composable
fun RowScope.StepBar(step: Int, total: Int) {
    val description = stringResource(R.string.step_of, step + 1, total)
    Row(
        Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier.weight(1f).height(7.dp)
                    .background(if (i <= step) SPColor.yellow else Color(0x55FFFFFF), RoundedCornerShape(9.dp)),
            )
        }
    }
}
