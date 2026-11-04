package com.santilega.scrollparty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.santilega.scrollparty.R
import com.santilega.scrollparty.ui.components.DisplayTitle
import com.santilega.scrollparty.ui.components.Machine
import com.santilega.scrollparty.ui.components.PlayGlyph
import com.santilega.scrollparty.ui.components.PlayerFace
import com.santilega.scrollparty.ui.components.PrimaryButton
import com.santilega.scrollparty.ui.components.ScreenScaffold
import com.santilega.scrollparty.ui.components.StepBar
import com.santilega.scrollparty.ui.components.Ticket
import com.santilega.scrollparty.ui.components.pop
import com.santilega.scrollparty.ui.components.rise
import com.santilega.scrollparty.ui.components.videoBackground
import com.santilega.scrollparty.ui.theme.SPColor
import com.santilega.scrollparty.ui.theme.SPShape
import com.santilega.scrollparty.ui.theme.SPSpace
import com.santilega.scrollparty.ui.theme.SPType

private const val STEPS = 4

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val last = step == STEPS - 1
    val title = stringResource(listOf(R.string.onb1_title, R.string.onb2_title, R.string.onb3_title, R.string.onb4_title)[step])
    val body = stringResource(listOf(R.string.onb1_body, R.string.onb2_body, R.string.onb3_body, R.string.onb4_body)[step])
    ScreenScaffold(
        bottom = { PrimaryButton(stringResource(if (last) R.string.get_started else R.string.next), { if (last) onDone() else step++ }) },
    ) {
        Row(Modifier.heightIn(min = SPSpace.minTouch), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SPSpace.s2)) {
            StepBar(step, STEPS)
            Box(Modifier.heightIn(min = SPSpace.minTouch).widthIn(min = SPSpace.minTouch).clickable(role = Role.Button, onClick = onDone), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.skip), style = SPType.body.copy(fontSize = 16.sp), color = SPColor.white)
            }
        }
        // key(step) reinicia las animaciones de entrada en cada paso.
        key(step) {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().heightIn(min = 260.dp), contentAlignment = Alignment.Center) { StepArt(step) }
                DisplayTitle(title, Modifier.fillMaxWidth().rise().padding(top = SPSpace.s1))
                Text(body, Modifier.rise().padding(top = SPSpace.s2), style = SPType.body, color = SPColor.white)
            }
        }
    }
}

@Composable
private fun StepArt(step: Int) {
    when (step) {
        0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Machine(Modifier.width(250.dp), lights = true) {
                Box(Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) {
                    Text("11", Modifier.pop(), style = SPType.display.copy(fontSize = 64.sp), color = SPColor.blue)
                }
            }
            Text(stringResource(R.string.reels_in_machine).uppercase(), Modifier.padding(top = SPSpace.s2), style = SPType.label, color = SPColor.white)
        }
        1 -> Box(
            Modifier.width(290.dp).height(180.dp).background(SPColor.night, RoundedCornerShape(12.dp)).border(7.dp, SPColor.white, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.pop().size(88.dp, 142.dp).clip(RoundedCornerShape(8.dp)).videoBackground(), contentAlignment = Alignment.Center) {
                PlayGlyph(56.dp)
            }
        }
        2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Machine(Modifier.width(250.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val name = stringResource(R.string.demo_name)
                    PlayerFace(name, 1, null, Modifier.size(62.dp))
                    Text(name, style = SPType.heading, color = SPColor.ink)
                }
            }
            Spacer(Modifier.height(SPSpace.s3))
            Ticket(stringResource(R.string.onb_ticket), Modifier.pop())
        }
        else -> Image(
            painterResource(R.drawable.ic_foreground), null,
            Modifier.pop().size(132.dp).clip(RoundedCornerShape(30.dp)).background(SPColor.blue).border(3.dp, SPColor.yellow, RoundedCornerShape(30.dp)),
        )
    }
}
