package com.santilega.scrollparty.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Único punto que lee el movimiento reducido (ANIMATOR_DURATION_SCALE = 0). */
val LocalReduceMotion = staticCompositionLocalOf { false }

object Motion {
    val spring = tween<Float>(400, easing = CubicBezierEasing(.3f, 1.7f, .5f, 1f))
    val snap = tween<Float>(320, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))
    const val PRESS_MS = 80
    const val FADE_MS = 150
    const val REEL_MS = 1500
    val reelEasing = CubicBezierEasing(.15f, .7f, .2f, 1.04f)
}

@Composable
fun ScrollPartyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduce by remember { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduce = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalReduceMotion provides reduce, content = content)
}
