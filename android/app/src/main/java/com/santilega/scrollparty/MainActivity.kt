package com.santilega.scrollparty

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val EMOJIS = listOf("😎", "🤠", "👻", "🦄", "🐸", "🍕")
private val Pink = Color(0xFFFF3D81)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Pink, onPrimary = Color.White, background = Color(0xFF120D1F), surface = Color(0xFF120D1F))) {
                Surface(Modifier.fillMaxSize()) { App(viewModel()) }
            }
        }
    }
}

@Composable
private fun App(vm: GameViewModel) {
    val group = vm.group
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            vm.uid == null -> Text("Loading…")
            group == null -> Home(vm)
            group.status == "collecting" -> Lobby(vm, group)
            group.status == "playing" -> Session(vm, group)
            else -> Results(vm)
        }
    }
}

@Composable
private fun Title(text: String) = Text(text, fontSize = 30.sp, fontWeight = FontWeight.Bold)

@Composable
private fun Home(vm: GameViewModel) {
    var name by remember { mutableStateOf(vm.name) }
    var emoji by remember { mutableStateOf(vm.emoji) }
    var code by remember { mutableStateOf("") }
    Title("ScrollParty")
    Text("Drop your best reels in the bag. Guess who sent what.")
    OutlinedTextField(name, { name = it.take(30) }, Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true)
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        EMOJIS.forEach {
            TextButton({ emoji = it }) { Text(it, fontSize = if (it == emoji) 30.sp else 20.sp) }
        }
    }
    Button({ vm.create(name.trim(), emoji) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank()) { Text("Create group") }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(code, { code = it.uppercase().take(6) }, Modifier.fillMaxWidth(), label = { Text("Group code") }, singleLine = true)
    OutlinedButton({ vm.joinGroup(code, name.trim(), emoji) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && code.length == 6) { Text("Join group") }
}

@Composable
private fun Lobby(vm: GameViewModel, group: Group) {
    var link by remember { mutableStateOf("") }
    Text("Group code", color = Color.Gray)
    Title(group.code)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            vm.members.forEach { Text("${it.emoji}  ${it.name}${if (it.id == group.adminId) " (host)" else ""}  ·  ${it.count} reels") }
        }
    }
    OutlinedTextField(link, { link = it }, Modifier.fillMaxWidth(), label = { Text("Paste a reel, TikTok or Short") }, singleLine = true)
    Button({ vm.addLink(link); link = "" }, Modifier.fillMaxWidth(), enabled = link.isNotBlank()) { Text("Add to the bag") }
    if (vm.myLinks.isNotEmpty()) Text("Your reels", color = Color.Gray)
    vm.myLinks.forEach { l ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(l.url, Modifier.weight(1f), maxLines = 1)
            TextButton({ vm.removeLink(l) }) { Text("Remove") }
        }
    }
    if (vm.isAdmin) {
        Button({ vm.start() }, Modifier.fillMaxWidth(), enabled = vm.members.size >= 2 && vm.members.any { it.count > 0 }) { Text("Start the party") }
    } else {
        Text("Waiting for the host to start…", color = Color.Gray)
    }
    TextButton({ vm.forget() }) { Text("Leave") }
}

@Composable
private fun Session(vm: GameViewModel, group: Group) {
    val round = vm.currentRound
    if (round == null) {
        Text("Getting the bag ready…")
        if (vm.isAdmin) TextButton({ vm.start() }) { Text("Retry") }
        return
    }
    val context = LocalContext.current
    val expected = vm.members.size - 1
    val author = vm.members.find { it.id == round.authorId }
    val revealed = round.authorId != null
    val mine = vm.myLinks.any { it.id == round.id }
    val myVote = round.votes[vm.uid]

    Text("Reel ${group.roundIndex + 1} of ${group.roundIds.size}", color = Color.Gray)
    Title(if (revealed) "It was ${author?.emoji.orEmpty()} ${author?.name ?: "someone"}!" else "Who sent this?")

    if (vm.isAdmin) {
        Button({
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(round.url)))
            } catch (_: ActivityNotFoundException) {
                vm.error = "No app can open this link"
            }
        }, Modifier.fillMaxWidth()) { Text("▶  Play reel") }
    }

    if (!revealed) {
        if (mine) {
            Text("This one's yours 🤫 Keep a straight face.", fontSize = 20.sp)
        } else {
            vm.members.filter { it.id != vm.uid }.forEach { m ->
                val selected = myVote == m.id
                OutlinedButton(
                    { vm.vote(round, m.id) },
                    Modifier.fillMaxWidth(),
                    colors = if (selected) ButtonDefaults.outlinedButtonColors(containerColor = Pink, contentColor = Color.White) else ButtonDefaults.outlinedButtonColors(),
                ) { Text("${m.emoji}  ${m.name}") }
            }
        }
        Text("${round.votes.size}/$expected have voted", fontSize = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    } else {
        if (!mine) Text(if (myVote == round.authorId) "You got it! +1" else "Fooled you!", fontSize = 20.sp)
        vm.members.filter { it.id in round.votes }.forEach { m ->
            val guess = vm.members.find { it.id == round.votes[m.id] }
            Text("${m.emoji} ${m.name} guessed ${guess?.name ?: "?"} ${if (guess?.id == round.authorId) "✅" else "❌"}")
        }
    }

    if (vm.isAdmin) {
        // Revela solo cuando han votado todos; el botón es para forzarlo si alguien tarda.
        LaunchedEffect(round.id, round.votes.size, revealed) {
            if (!revealed && expected > 0 && round.votes.size >= expected) vm.reveal(round)
        }
        if (revealed) Button({ vm.next() }, Modifier.fillMaxWidth()) { Text("Next") }
        else TextButton({ vm.reveal(round) }, Modifier.fillMaxWidth()) { Text("Reveal now") }
    }
}

@Composable
private fun Award(title: String, ids: List<String>, vm: GameViewModel) {
    if (ids.isEmpty()) return
    Text("$title: ${ids.joinToString { id -> vm.members.find { it.id == id }?.let { "${it.emoji} ${it.name}" }.orEmpty() }}", fontSize = 18.sp)
}

@Composable
private fun Results(vm: GameViewModel) {
    val table = standings(vm.rounds, vm.members.map { it.id })
    Title("Final scores")
    Award("🕵️ Detective", winners(table) { it.correct }, vm)
    Award("🥷 Infiltrator", winners(table) { it.fooled }, vm)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            table.forEachIndexed { i, s ->
                val m = vm.members.find { it.id == s.memberId }
                Text("${i + 1}. ${m?.emoji.orEmpty()} ${m?.name.orEmpty()} — ${s.score}", fontSize = 18.sp)
            }
        }
    }
    TextButton({ vm.forget() }) { Text("Leave") }
}
