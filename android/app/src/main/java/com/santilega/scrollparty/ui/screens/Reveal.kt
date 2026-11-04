package com.santilega.scrollparty.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.GameViewModel
import com.santilega.scrollparty.Group
import com.santilega.scrollparty.R
import com.santilega.scrollparty.Round
import com.santilega.scrollparty.ui.components.Confetti
import com.santilega.scrollparty.ui.components.DisplayTitle
import com.santilega.scrollparty.ui.components.PlayerFace
import com.santilega.scrollparty.ui.components.PrimaryButton
import com.santilega.scrollparty.ui.components.ReelReveal
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.components.Ticket
import com.santilega.scrollparty.ui.components.pop
import com.santilega.scrollparty.ui.components.rise
import com.santilega.scrollparty.ui.theme.LocalReduceMotion
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/** Revelado: rodillo único con caras, háptica, confeti, tique y quién votó qué. «Next reel» solo el anfitrión. */
@Composable
fun RevealScreen(vm: GameViewModel, group: Group, round: Round) {
    val author = vm.members.find { it.id == round.authorId }
    val reduce = LocalReduceMotion.current
    var landed by remember(round.id) { mutableStateOf(false) }
    val pool = remember(round.id, author?.id) { vm.members.filter { it.id != author?.id }.shuffled() }
    val myVote = round.votes[vm.uid]
    val voters = vm.members.filter { it.id in round.votes && it.id != author?.id }
    val last = group.roundIndex + 1 >= group.roundIds.size
    ScreenScaffold(
        bottom = {
            if (vm.isAdmin) PrimaryButton(stringResource(if (last) R.string.see_final else R.string.next_reel), { vm.next() })
            else Text(stringResource(R.string.waiting_next), Modifier.fillMaxWidth().padding(vertical = SPSpace.s2), style = SPType.body, color = SPColor.white, textAlign = TextAlign.Center)
        },
        overlay = { if (landed && !reduce) Confetti() },
    ) {
        if (author == null) return@ScreenScaffold
        ReelReveal(pool, author, { landed = true }, Modifier.widthIn(max = 290.dp).fillMaxWidth().align(Alignment.CenterHorizontally))
        Column(Modifier.fillMaxWidth().graphicsLayer { alpha = if (landed) 1f else 0f }, horizontalAlignment = Alignment.CenterHorizontally) {
            if (landed) DisplayTitle(stringResource(R.string.author_reveal, author.name), Modifier.pop().padding(top = SPSpace.s3), align = TextAlign.Center)
            else DisplayTitle(".", Modifier.padding(top = SPSpace.s3), color = SPColor.blue, shadow = false)
            val fooledCount = round.votes.count { (voter, guess) -> voter != author.id && guess != author.id }
            when {
                round.authorId == vm.uid -> Ticket(pluralStringResource(R.plurals.reveal_mine, fooledCount, fooledCount), Modifier.padding(top = 6.dp))
                myVote == author.id -> Ticket(stringResource(R.string.reveal_right), Modifier.padding(top = 6.dp))
                else -> Ticket(stringResource(R.string.reveal_wrong), Modifier.padding(top = 6.dp), fooled = true)
            }
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                voters.forEach { voter ->
                    val guess = vm.members.find { it.id == round.votes[voter.id] }
                    val right = guess?.id == author.id
                    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).rise(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        PlayerFace(voter.name, voter.color, voter.photo, Modifier.size(42.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = SPColor.white)) { append(voter.name) }
                                append(" ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = SPColor.soft)) { append(stringResource(R.string.guessed, guess?.name ?: "?")) }
                            },
                            Modifier.weight(1f), style = SPType.body.copy(fontSize = 18.sp),
                        )
                        Ticket(stringResource(if (right) R.string.right else R.string.fooled), fooled = !right, small = true)
                    }
                }
            }
        }
    }
}
