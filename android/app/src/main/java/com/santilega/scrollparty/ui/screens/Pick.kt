package com.santilega.scrollparty.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.GameViewModel
import com.santilega.scrollparty.Group
import com.santilega.scrollparty.Member
import com.santilega.scrollparty.R
import com.santilega.scrollparty.Round
import com.santilega.scrollparty.ui.components.ClueCard
import com.santilega.scrollparty.ui.components.DisplayTitle
import com.santilega.scrollparty.ui.components.Machine
import com.santilega.scrollparty.ui.components.PlayerFace
import com.santilega.scrollparty.ui.components.PrimaryButton
import com.santilega.scrollparty.ui.components.ReactionBar
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.components.SuspectWheel
import com.santilega.scrollparty.ui.components.pop
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/** Elegir (votantes): pista, rueda de sospechosos, reacciones y «That's my guess»; se puede cambiar hasta el revelado. */
@Composable
fun PickScreen(vm: GameViewModel, group: Group, round: Round) = key(round.id) {
    val others = vm.members.filter { it.id != vm.uid }
    val myVote = round.votes[vm.uid]
    var selected by remember { mutableStateOf<Member?>(others.firstOrNull { it.id == myVote } ?: others.firstOrNull()) }
    var locked by remember { mutableStateOf(myVote != null) }
    val choose = stringResource(R.string.choose_who)
    ScreenScaffold(
        bottom = {
            ReactionBar(vm.uid?.let { round.reactions[it] }, { vm.react(round, it) })
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                selected?.takeIf { locked }?.let { stringResource(R.string.guess_locked, it.name) } ?: stringResource(R.string.my_guess),
                {
                    val m = selected
                    if (locked) locked = false else if (m != null) { vm.vote(round, m.id); locked = true }
                },
                faceColor = if (locked) SPColor.mint else null,
                enabled = selected != null,
            )
        },
    ) {
        Text(
            stringResource(R.string.reel_of_watch, group.roundIndex + 1, group.roundIds.size).uppercase(),
            Modifier.fillMaxWidth().padding(top = SPSpace.s1), style = SPType.label.copy(fontSize = 16.sp), color = SPColor.yellow, textAlign = TextAlign.Center,
        )
        if (round.clue.isNotBlank()) ClueCard(stringResource(R.string.clue_card_label), round.clue, Modifier.padding(top = 12.dp))
        DisplayTitle(stringResource(R.string.who_sent_it), Modifier.fillMaxWidth().padding(top = SPSpace.s2, bottom = 12.dp), style = SPType.display.copy(fontSize = 38.sp))
        Machine(Modifier.fillMaxWidth()) {
            Box(Modifier.semantics { contentDescription = choose }) {
                SuspectWheel(
                    others, others.indexOfFirst { it.id == myVote }.coerceAtLeast(0),
                    { m -> if (m.id != selected?.id) { selected = m; locked = false } },
                )
            }
        }
    }
}

/** Es tuyo (autor): su cara, sin voto, con reacciones. */
@Composable
fun YoursScreen(vm: GameViewModel, round: Round) {
    val me = vm.me
    ScreenScaffold(bottom = { ReactionBar(vm.uid?.let { round.reactions[it] }, { vm.react(round, it) }) }) {
        Spacer(Modifier.height(SPSpace.s4))
        if (me != null) {
            Machine(Modifier.widthIn(max = 230.dp).fillMaxWidth().align(Alignment.CenterHorizontally).pop()) {
                PlayerFace(me.name, me.color, me.photo, Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp), contentDescription = me.name)
            }
        }
        DisplayTitle(stringResource(R.string.this_ones_yours), Modifier.fillMaxWidth().padding(top = SPSpace.s4), align = TextAlign.Center)
        Text(stringResource(R.string.yours_body), Modifier.fillMaxWidth().padding(top = 12.dp), style = SPType.body, color = SPColor.white, textAlign = TextAlign.Center)
    }
}
