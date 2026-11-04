package com.santilega.scrollparty.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.GameViewModel
import com.santilega.scrollparty.Group
import com.santilega.scrollparty.MAX_CLUE
import com.santilega.scrollparty.R
import com.santilega.scrollparty.ui.components.AppTextField
import com.santilega.scrollparty.ui.components.Machine
import com.santilega.scrollparty.ui.components.PlayerFace
import com.santilega.scrollparty.ui.components.PrimaryButton
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.components.pop
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/** Máquina (bolsa): código, invitar, contador, jugadores, añadir reel con pista y empezar (anfitrión). */
@Composable
fun LobbyScreen(vm: GameViewModel, group: Group) {
    val context = LocalContext.current
    var adding by rememberSaveable { mutableStateOf(false) }
    var link by rememberSaveable { mutableStateOf("") }
    var clue by rememberSaveable { mutableStateOf("") }
    val total = vm.members.sumOf { it.count }
    val canStart = vm.members.size >= 2 && total > 0
    val invite = stringResource(R.string.invite_text, group.code)
    val chooserTitle = stringResource(R.string.invite_chooser)

    ScreenScaffold(
        bottom = {
            PrimaryButton(stringResource(R.string.add_reel), { adding = !adding }, alt = true)
            Spacer(Modifier.height(10.dp))
            if (vm.isAdmin) {
                if (!canStart) Text(stringResource(R.string.need_players), Modifier.fillMaxWidth().padding(bottom = SPSpace.s1), style = SPType.caption, color = SPColor.soft, textAlign = TextAlign.Center)
                PrimaryButton(stringResource(R.string.start_party), { vm.start() }, enabled = canStart)
            } else {
                Text(stringResource(R.string.waiting_host), Modifier.fillMaxWidth().padding(vertical = SPSpace.s1), style = SPType.body, color = SPColor.white, textAlign = TextAlign.Center)
            }
        },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = SPSpace.minTouch), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.party_code).uppercase(), style = SPType.label, color = SPColor.soft)
                Text(group.code, style = SPType.heading.copy(fontSize = 26.sp, letterSpacing = 0.04.em), color = SPColor.yellow)
            }
            Box(
                Modifier.heightIn(min = SPSpace.minTouch).background(SPColor.white, SPShape.r1)
                    .clickable(role = Role.Button) {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, invite)
                        context.startActivity(Intent.createChooser(send, chooserTitle))
                    }
                    .padding(horizontal = SPSpace.s2),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.invite), style = SPType.body.copy(fontSize = 16.sp), color = SPColor.ink) }
        }
        Machine(Modifier.fillMaxWidth().padding(top = 14.dp), lights = true) {
            Column(Modifier.fillMaxWidth().heightIn(min = 226.dp).padding(SPSpace.s2), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(total.toString(), Modifier.pop(), style = SPType.display.copy(fontSize = 96.sp, lineHeight = 100.sp), color = SPColor.blue)
                Text(stringResource(R.string.reels_in_machine), style = SPType.body.copy(fontSize = 16.sp), color = SPColor.ink, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = SPSpace.s3, bottom = SPSpace.s1), Arrangement.SpaceBetween) {
            Text(pluralStringResource(R.plurals.players, vm.members.size, vm.members.size).uppercase(), style = SPType.label, color = SPColor.soft)
            Text(stringResource(R.string.reels_sent).uppercase(), style = SPType.label, color = SPColor.soft)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            vm.members.forEach { m ->
                Column(Modifier.width(64.dp).semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerFace(m.name, m.color, m.photo, Modifier.size(58.dp))
                    Text(if (m.id == vm.uid) stringResource(R.string.you) else m.name, Modifier.padding(top = 4.dp), style = SPType.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = SPColor.white, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(m.count.toString(), style = SPType.caption, color = SPColor.soft)
                }
            }
        }
        if (adding) {
            Column(Modifier.fillMaxWidth().padding(top = SPSpace.s3).background(SPColor.white, SPShape.r2).padding(SPSpace.s2), verticalArrangement = Arrangement.spacedBy(SPSpace.s2)) {
                AppTextField(link, { link = it }, stringResource(R.string.reel_link_label), labelColor = SPColor.inkSoft, tag = "link")
                AppTextField(clue, { clue = it.take(MAX_CLUE) }, stringResource(R.string.clue_label), labelColor = SPColor.inkSoft, tag = "clue")
                PrimaryButton(stringResource(R.string.add_to_machine), { vm.addLink(link, clue) { link = ""; clue = ""; adding = false } }, enabled = link.isNotBlank())
            }
        }
        if (vm.myLinks.isNotEmpty()) {
            Text(stringResource(R.string.your_reels).uppercase(), Modifier.padding(top = SPSpace.s3, bottom = SPSpace.s1), style = SPType.label, color = SPColor.soft)
            vm.myLinks.forEach { l ->
                Row(Modifier.fillMaxWidth().heightIn(min = SPSpace.minTouch), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(l.url, style = SPType.body.copy(fontSize = 16.sp), color = SPColor.white, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (l.clue.isNotEmpty()) Text("“${l.clue}”", style = SPType.caption, color = SPColor.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val description = stringResource(R.string.remove_reel, l.url)
                    Box(
                        Modifier.heightIn(min = SPSpace.minTouch).widthIn(min = SPSpace.minTouch).clickable(role = Role.Button) { vm.removeLink(l) }.semantics { contentDescription = description },
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.remove), style = SPType.body.copy(fontSize = 16.sp), color = SPColor.white) }
                }
            }
        }
        Spacer(Modifier.height(SPSpace.s2))
    }
}
