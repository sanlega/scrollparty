package com.santilega.scrollparty

import androidx.compose.ui.graphics.Color
import com.santilega.scrollparty.ui.theme.PlayerColors
import com.santilega.scrollparty.ui.theme.SPColor
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Pares de color de docs/design/sistema.md: texto 4,5:1 y controles 3:1 como mínimo. */
class ContrastTest {
    private fun channel(c: Float) = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    private fun luminance(c: Color) = 0.2126f * channel(c.red) + 0.7152f * channel(c.green) + 0.0722f * channel(c.blue)
    private fun ratio(a: Color, b: Color): Float {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    private fun check(min: Float, vararg pairs: Pair<Pair<String, Color>, Pair<String, Color>>) {
        val failing = pairs.filter { ratio(it.first.second, it.second.second) < min }
            .map { "${it.first.first} sobre ${it.second.first}: %.2f".format(ratio(it.first.second, it.second.second)) }
        assertTrue("Por debajo de $min:1: $failing", failing.isEmpty())
    }

    private infix fun String.on(other: String) = this to other
    private val colors = mapOf(
        "ink" to SPColor.ink, "inkSoft" to SPColor.inkSoft, "soft" to SPColor.soft, "white" to SPColor.white,
        "blue" to SPColor.blue, "night" to SPColor.night, "night2" to SPColor.night2, "yellow" to SPColor.yellow,
        "mint" to SPColor.mint, "blush" to SPColor.blush, "coral" to SPColor.coral,
    )
    private fun pair(text: String, bg: String) = (text to colors.getValue(text)) to (bg to colors.getValue(bg))

    @Test fun textoConContrasteSuficiente() = check(
        4.5f,
        pair("ink", "yellow"), pair("ink", "white"), pair("ink", "mint"), pair("ink", "blush"),
        pair("inkSoft", "white"), pair("soft", "blue"), pair("soft", "night"), pair("soft", "night2"),
        pair("white", "blue"), pair("white", "night"), pair("yellow", "blue"), pair("yellow", "night"),
        pair("blue", "white"), // el contador de la máquina
    )

    @Test fun lasInicialesSeLeenSobreLosDoceColoresDeJugador() {
        val failing = PlayerColors.withIndex().filter { ratio(SPColor.ink, it.value) < 4.5f }.map { it.index }
        assertTrue("ink sin contraste sobre los colores $failing", failing.isEmpty())
    }

    @Test fun controlesConContrasteSuficiente() = check(
        3f,
        pair("coral", "white"), // barra de selección de la rueda
        pair("white", "blue"), pair("yellow", "blue"), pair("soft", "night"), // botón, borde del voto secreto
    )

    @Test fun blancoSobreCoralEstaProhibido() {
        assertTrue("Si blanco sobre coral pasara 4,5:1 la regla del sistema sobraría", ratio(SPColor.white, SPColor.coral) < 4.5f)
    }
}
