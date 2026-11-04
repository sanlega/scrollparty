package com.santilega.scrollparty.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.santilega.scrollparty.GameViewModel
import com.santilega.scrollparty.Group
import com.santilega.scrollparty.R
import com.santilega.scrollparty.ui.components.DisplayTitle
import com.santilega.scrollparty.ui.components.PrimaryButton
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.components.Toast
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPType
import com.santilega.scrollparty.ui.theme.ScrollPartyTheme
import kotlinx.coroutines.delay

/** Raíz: elige pantalla según el estado de la sesión y el rol. [notice] es un mensaje que trae un Compartir fallido. */
@Composable
fun App(vm: GameViewModel, @StringRes notice: Int?, onNoticeShown: () -> Unit) {
    ScrollPartyTheme {
        val group = vm.group
        Box(Modifier.fillMaxSize()) {
            when {
                !vm.ready -> Splash()
                !vm.onboarded -> OnboardingScreen(vm::finishOnboarding)
                vm.name.isBlank() -> ProfileScreen(vm)
                group == null && vm.hasSavedParty -> Splash()
                group == null -> HomeScreen(vm)
                group.status == "collecting" -> LobbyScreen(vm, group)
                group.status == "playing" -> SessionScreen(vm, group)
                else -> FinalScreen(vm)
            }
            val message = vm.error ?: notice?.let { stringResource(it) }
            if (message != null) {
                LaunchedEffect(message) {
                    delay(4_500)
                    vm.error = null
                    onNoticeShown()
                }
                Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.TopCenter) { Toast(message) }
            }
        }
    }
}

@Composable
private fun Splash() = ScreenScaffold {
    DisplayTitle(stringResource(R.string.app_name), Modifier.padding(top = 96.dp))
    Text(stringResource(R.string.loading), style = SPType.body, color = SPColor.white)
}

/** Una ronda en curso: la pantalla depende de si ya se reveló y del rol (anfitrión, autor, votante). */
@Composable
private fun SessionScreen(vm: GameViewModel, group: Group) {
    val round = vm.currentRound
    when {
        round == null -> ScreenScaffold(bottom = if (vm.isAdmin) ({ PrimaryButton(stringResource(R.string.retry), { vm.start() }) }) else null) {
            DisplayTitle(stringResource(R.string.machine_getting_ready), Modifier.padding(top = 96.dp))
        }
        round.authorId != null -> RevealScreen(vm, group, round)
        vm.isAdmin -> StageScreen(vm, group, round)
        vm.myLinks.any { it.id == round.id } -> YoursScreen(vm, round)
        else -> PickScreen(vm, group, round)
    }
}
