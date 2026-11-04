package com.santilega.scrollparty

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.snapshotFlow
import com.santilega.scrollparty.ui.components.Toast
import com.santilega.scrollparty.ui.theme.ScrollPartyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val SHORTCUT_ID = "party"
private const val SHARE_CATEGORY = "com.santilega.scrollparty.category.SHARE_TARGET"
const val EXTRA_NOTICE = "notice"

/** Sharing Shortcut de la fiesta actual: sale en la hoja de compartir con el código. */
fun publishPartyShortcut(context: Context, code: String) {
    val open = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW)
    val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
        .setShortLabel(code)
        .setLongLabel(context.getString(R.string.shortcut_long, code))
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setCategories(setOf(SHARE_CATEGORY))
        .setIntent(open)
        .setLongLived(true)
        .build()
    runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }
}

fun removePartyShortcut(context: Context) {
    runCatching { ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(SHORTCUT_ID)) }
}

/**
 * Destino de Compartir. Transparente: si hay fiesta en `collecting` mete el enlace y muestra un Toast;
 * si no, abre la app con un mensaje claro.
 */
class ShareActivity : ComponentActivity() {
    private var toast by mutableStateOf<Pair<String, String?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrollPartyTheme {
                Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
                    toast?.let { (title, detail) -> Toast(title, detail = detail) }
                }
            }
        }
        val vm = ViewModelProvider(this)[GameViewModel::class.java]
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        lifecycleScope.launch {
            val notice = when {
                extractLink(text) == null -> R.string.share_no_link
                else -> {
                    val group = if (vm.hasSavedParty) withTimeoutOrNull(8_000) { snapshotFlow { vm.group }.filterNotNull().first() } else null
                    when {
                        group == null -> R.string.share_no_party
                        group.status != "collecting" -> R.string.share_not_collecting
                        else -> {
                            val saved = runCatching { vm.addLinkNow(text, "") }
                            if (saved.isSuccess) {
                                toast = getString(R.string.shared_title) to getString(R.string.shared_detail, group.code)
                                delay(2_400)
                                null
                            } else R.string.error_generic
                        }
                    }
                }
            }
            if (notice != null) {
                startActivity(Intent(this@ShareActivity, MainActivity::class.java).putExtra(EXTRA_NOTICE, notice))
            }
            finish()
        }
    }
}
