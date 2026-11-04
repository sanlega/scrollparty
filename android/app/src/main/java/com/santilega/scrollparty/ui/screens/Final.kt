package com.santilega.scrollparty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santilega.scrollparty.GameViewModel
import com.santilega.scrollparty.R
import com.santilega.scrollparty.reelOfTheNight
import com.santilega.scrollparty.standings
import com.santilega.scrollparty.winners
import com.santilega.scrollparty.ui.components.DisplayTitle
import com.santilega.scrollparty.ui.components.PlayerFace
import com.santilega.scrollparty.ui.components.PrizeCard
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.components.rise
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

/** Final: tiques por jugador y los premios Top Detective, Master Bluffer y Reel of the Night. */
@Composable
fun FinalScreen(vm: GameViewModel) {
    val table = standings(vm.rounds, vm.members.map { it.id })
    val byId = vm.members.associateBy { it.id }
    val prizes = listOf(
        R.string.prize_detective to winners(table) { it.correct },
        R.string.prize_bluffer to winners(table) { it.fooled },
        R.string.prize_reel to listOfNotNull(reelOfTheNight(vm.rounds)),
    ).filter { it.second.isNotEmpty() }
    val tall = LocalDensity.current.fontScale > 1.3f // con letra grande, los premios van en columna
    ScreenScaffold {
        DisplayTitle(stringResource(R.string.final_title), Modifier.fillMaxWidth().padding(top = SPSpace.s2), align = TextAlign.Center)
        Column(
            Modifier.fillMaxWidth().padding(top = 18.dp).rise().graphicsLayer { rotationZ = -1.4f }
                .background(SPColor.white, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(14.dp),
        ) {
            table.forEachIndexed { i, s ->
                val m = byId[s.memberId] ?: return@forEachIndexed
                val label = stringResource(R.string.tickets_count, m.name, s.score)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 46.dp).semantics(mergeDescendants = true) { contentDescription = label },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("${i + 1}", Modifier.width(18.dp), style = SPType.body.copy(fontSize = 17.sp), color = SPColor.ink)
                    PlayerFace(m.name, m.color, m.photo, Modifier.size(38.dp))
                    Text(if (m.id == vm.uid) stringResource(R.string.you) else m.name, Modifier.weight(1f), style = SPType.body.copy(fontSize = 17.sp), color = SPColor.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.score.toString(), Modifier.testTag("score-${m.id}"), style = SPType.heading, color = SPColor.ink)
                }
            }
        }
        Spacer(Modifier.height(SPSpace.s3))
        if (tall) {
            Column(verticalArrangement = Arrangement.spacedBy(SPSpace.s1)) {
                prizes.forEach { (title, ids) -> Prize(title, ids.mapNotNull { byId[it] }, Modifier.fillMaxWidth()) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(SPSpace.s1)) {
                prizes.forEach { (title, ids) -> Prize(title, ids.mapNotNull { byId[it] }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Prize(title: Int, winners: List<com.santilega.scrollparty.Member>, modifier: Modifier) {
    PrizeCard(
        stringResource(title), winners.joinToString { it.name },
        { winners.firstOrNull()?.let { PlayerFace(it.name, it.color, it.photo, Modifier.size(46.dp)) } },
        modifier.rise(),
    )
}
