package com.santilega.scrollparty.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import com.santilega.scrollparty.GameViewModel
import com.santilega.scrollparty.Group
import com.santilega.scrollparty.R
import com.santilega.scrollparty.Round
import com.santilega.scrollparty.ui.components.DisplayTitle
import com.santilega.scrollparty.ui.components.FlyingReactions
import com.santilega.scrollparty.ui.components.Machine
import com.santilega.scrollparty.ui.components.PlayGlyph
import com.santilega.scrollparty.ui.components.PlayerFace
import com.santilega.scrollparty.ui.components.PrimaryButton
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.components.videoBackground
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/**
 * Tele (anfitrión): vídeo grande, «N of M voted», voto secreto y reacciones. El texto del voto
 * es el mismo sea quien sea el elegido: aquí nunca se resalta ni se nombra la selección.
 */
@Composable
fun StageScreen(vm: GameViewModel, group: Group, round: Round) {
    val context = LocalContext.current
    var voting by rememberSaveable(round.id) { mutableStateOf(false) }
    val mine = vm.myLinks.any { it.id == round.id }
    val voted = vm.uid in round.votes
    val expected = vm.members.size - 1
    if (voting) {
        BackHandler { voting = false }
        SecretVoteScreen(vm, round) { voting = false }
        return
    }
    val noApp = stringResource(R.string.no_app_for_link)
    ScreenScaffold(
        night = true,
        overlay = { FlyingReactions(round.reactions) },
        bottom = {
            if (mine) {
                Text(stringResource(R.string.this_ones_yours), Modifier.fillMaxWidth(), style = SPType.body, color = SPColor.soft, textAlign = TextAlign.Center)
            } else {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).border(2.dp, SPColor.soft, SPShape.r1)
                        .clickable(role = Role.Button) { voting = true }
                        .testTag("secretVote"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(if (voted) R.string.vote_in else R.string.cast_vote), Modifier.padding(horizontal = SPSpace.s2, vertical = 12.dp), style = SPType.body.copy(fontSize = 17.sp), color = SPColor.white, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(SPSpace.s2))
            PrimaryButton(stringResource(R.string.reveal_who), { vm.reveal(round) })
        },
    ) {
        Text(
            stringResource(R.string.reel_of, group.roundIndex + 1, group.roundIds.size).uppercase(),
            Modifier.fillMaxWidth().padding(top = SPSpace.s1), style = SPType.label.copy(fontSize = 16.sp), color = SPColor.yellow, textAlign = TextAlign.Center,
        )
        Machine(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 306.dp).fillMaxWidth().padding(top = SPSpace.s1), lights = true) {
            Box(Modifier.fillMaxWidth().heightIn(min = 372.dp).videoBackground(), contentAlignment = Alignment.Center) {
                val label = stringResource(R.string.play_reel)
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(Color(0x99000000))
                        .clickable(role = Role.Button) {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(round.url))) } catch (_: ActivityNotFoundException) { vm.error = noApp }
                        }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) { PlayGlyph(64.dp) }
            }
        }
        val status = stringResource(R.string.voted_of, round.votes.size, expected)
        val count = round.votes.size.toString()
        Text(
            highlight(status, count, SPColor.yellow),
            Modifier.fillMaxWidth().padding(top = SPSpace.s3).testTag("voteStatus"),
            style = SPType.title.copy(color = SPColor.white), textAlign = TextAlign.Center,
        )
    }
}

private fun highlight(text: String, part: String, color: Color) = buildAnnotatedString {
    val i = text.indexOf(part)
    if (i < 0) {
        append(text)
    } else {
        append(text.substring(0, i))
        withStyle(SpanStyle(color = color)) { append(part) }
        append(text.substring(i + part.length))
    }
}

/** Rejilla neutra: caras sin resaltar. Al votar vuelve a la tele sin indicar a quién. */
@Composable
private fun SecretVoteScreen(vm: GameViewModel, round: Round, onDone: () -> Unit) {
    val candidates = vm.members.filter { it.id != vm.uid }
    ScreenScaffold(night = true) {
        Spacer(Modifier.height(SPSpace.s2))
        DisplayTitle(stringResource(R.string.secret_vote), Modifier.fillMaxWidth(), style = SPType.title, align = TextAlign.Center)
        Text(stringResource(R.string.secret_vote_hint), Modifier.fillMaxWidth().padding(top = 6.dp, bottom = SPSpace.s3), style = SPType.body, color = SPColor.white, textAlign = TextAlign.Center)
        candidates.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m ->
                    val description = stringResource(R.string.vote_for, m.name)
                    Column(
                        Modifier.weight(1f).clickable(role = Role.Button) { vm.vote(round, m.id); onDone() }.clearAndSetSemantics { contentDescription = description },
                    ) {
                        PlayerFace(m.name, m.color, m.photo, Modifier.fillMaxWidth().aspectRatio(1f))
                        Text(m.name, Modifier.fillMaxWidth().heightIn(min = SPSpace.s1 * 3), style = SPType.caption.copy(fontSize = 14.sp), color = SPColor.soft, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
