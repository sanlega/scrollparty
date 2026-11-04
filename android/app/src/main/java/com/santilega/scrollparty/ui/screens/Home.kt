package com.santilega.scrollparty.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.santilega.scrollparty.GameViewModel
import com.santilega.scrollparty.R
import com.santilega.scrollparty.ui.components.AppTextField
import com.santilega.scrollparty.ui.components.DisplayTitle
import com.santilega.scrollparty.ui.components.PlayerFace
import com.santilega.scrollparty.ui.components.PrimaryButton
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/** Inicio: crear una fiesta o unirse con el código. */
@Composable
fun HomeScreen(vm: GameViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    ScreenScaffold(
        bottom = {
            PrimaryButton(stringResource(R.string.create_party), { vm.create() })
            Spacer(Modifier.height(SPSpace.s2))
            AppTextField(
                code, { code = it.uppercase().take(6) }, stringResource(R.string.party_code), tag = "code",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
            Spacer(Modifier.height(SPSpace.s2))
            PrimaryButton(stringResource(R.string.join_party), { vm.joinGroup(code) }, alt = true, enabled = code.length == 6)
        },
    ) {
        Spacer(Modifier.height(SPSpace.s4))
        DisplayTitle(stringResource(R.string.app_name), Modifier.fillMaxWidth())
        Text(stringResource(R.string.home_tagline), Modifier.fillMaxWidth(), style = SPType.body, color = SPColor.white)
        Spacer(Modifier.height(SPSpace.s3))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SPSpace.s2)) {
            PlayerFace(vm.name, 5, vm.photo, Modifier.size(56.dp))
            Text(stringResource(R.string.hello_name, vm.name), style = SPType.heading, color = SPColor.white)
        }
    }
}
