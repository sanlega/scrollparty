package com.santilega.scrollparty

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.santilega.scrollparty.ui.screens.App

class MainActivity : ComponentActivity() {
    private var notice by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fondos azules u oscuros: iconos de las barras siempre claros.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        readNotice(intent)
        setContent { App(viewModel(), notice) { notice = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readNotice(intent)
    }

    private fun readNotice(intent: Intent) {
        intent.getIntExtra(EXTRA_NOTICE, 0).takeIf { it != 0 }?.let { notice = it }
    }
}
