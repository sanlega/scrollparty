package com.santilega.scrollparty.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.R

/** Tokens de docs/design/sistema.md; iguales en iOS. */
object SPColor {
    val blue = Color(0xFF2323FF)
    val blueDeep = Color(0xFF1616C9)
    val night = Color(0xFF0B0B3B)
    val night2 = Color(0xFF17175C)
    val yellow = Color(0xFFFFE600)
    val yellowDeep = Color(0xFFB3A000)
    val coral = Color(0xFFFF4D2E)
    val mint = Color(0xFF16E0A6)
    val blush = Color(0xFFFFC9BF)
    val white = Color(0xFFFFFFFF)
    val ink = Color(0xFF14143C)
    val inkSoft = Color(0xFF4A4A75)
    val soft = Color(0xFFD9D9FF)
    val whiteLip = Color(0xFF9C9CC9)
}

val PlayerColors = listOf(
    0xFFFF4D2E, 0xFF16E0A6, 0xFFFF4FD8, 0xFFFFB400, 0xFF2FB6FF, 0xFFFFE600,
    0xFFB06BFF, 0xFF7BEA3A, 0xFFFF7A59, 0xFF3DE0E0, 0xFFFF5C9E, 0xFFFFC14D,
).map { Color(it) }

fun playerColor(index: Int) = PlayerColors[index.mod(PlayerColors.size)]

object SPSpace {
    val s1 = 8.dp
    val s2 = 16.dp
    val s3 = 24.dp
    val s4 = 32.dp
    val margin = 24.dp
    val minTouch = 48.dp
    val buttonHeight = 64.dp
    val lip = 7.dp
}

object SPShape {
    val r1 = RoundedCornerShape(14.dp)
    val r2 = RoundedCornerShape(22.dp)
    val r3 = RoundedCornerShape(30.dp)
}

private val Bowlby = FontFamily(Font(R.font.bowlby_one))
private val DmSans = FontFamily(Font(R.font.dm_sans_medium, FontWeight.Medium), Font(R.font.dm_sans_bold, FontWeight.Bold))

object SPType {
    val display = TextStyle(fontFamily = Bowlby, fontSize = 46.sp, lineHeight = 48.sp)
    val title = TextStyle(fontFamily = Bowlby, fontSize = 34.sp, lineHeight = 36.sp)
    val button = TextStyle(fontFamily = Bowlby, fontSize = 21.sp, lineHeight = 24.sp)
    val heading = TextStyle(fontFamily = Bowlby, fontSize = 22.sp, lineHeight = 24.sp)
    val body = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 27.sp)
    val label = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.06.em)
    val caption = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
}
