package com.santilega.scrollparty

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val TIMEOUT = 25_000L
private const val SHOTS = "Pictures/sp-android" // MediaStore: sobrevive a la desinstalación; se recoge con adb pull /sdcard/Pictures/sp-android
private const val CLUE = "my mum sent me this"

/** Otro móvil: un cliente Firebase con su propia sesión anónima, sujeto a las mismas reglas. */
private class Friend(tag: String, val name: String, private val color: Int, context: Context, private val photo: String? = null) {
    private val app = FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder().setApiKey("demo-key").setProjectId("demo-scrollparty").setApplicationId("1:1:android:1").build(),
        tag + System.nanoTime(),
    )
    private val db = FirebaseFirestore.getInstance(app).apply { useEmulator("10.0.2.2", 8180) }
    val uid: String = FirebaseAuth.getInstance(app).run {
        useEmulator("10.0.2.2", 9099)
        Tasks.await(signInAnonymously()).user!!.uid
    }
    var linkId = ""

    private fun group(code: String) = db.collection("groups").document(code)
    private fun round(code: String, id: String) = group(code).collection("rounds").document(id)

    fun join(code: String) {
        val data = mutableMapOf<String, Any>("name" to name, "emoji" to "", "color" to color, "count" to 0, "joinedAt" to System.currentTimeMillis())
        photo?.let { data["photo"] = it }
        Tasks.await(group(code).collection("members").document(uid).set(data))
    }

    fun addLink(code: String, url: String, clue: String? = null) {
        val doc = group(code)
        val link = doc.collection("links").document()
        linkId = link.id
        val data = mutableMapOf<String, Any>("url" to url, "authorId" to uid)
        clue?.let { data["clue"] = it }
        Tasks.await(
            db.batch().set(link, data)
                .update(doc.collection("members").document(uid), "count", FieldValue.increment(1))
                .commit(),
        )
    }

    fun roundIds(code: String) = Tasks.await(group(code).get()).get("roundIds") as List<*>
    fun vote(code: String, round: Any?, guess: String) { Tasks.await(round(code, round as String).update("votes.$uid", guess)) }
    fun react(code: String, round: Any?, reaction: String) { Tasks.await(round(code, round as String).update("reactions.$uid", reaction)) }

    // Papel de anfitrión, para la partida en la que el móvil de pruebas es un invitado.
    fun createGroup(): String {
        val code = newCode()
        Tasks.await(group(code).set(mapOf("adminId" to uid, "status" to "collecting", "roundIds" to emptyList<String>(), "roundIndex" to 0)))
        join(code)
        return code
    }

    fun start(code: String) {
        val doc = group(code)
        Tasks.await(doc.update("status", "playing"))
        val links = Tasks.await(doc.collection("links").get()).documents.shuffled()
        val batch = db.batch()
        for (l in links) {
            batch.set(
                doc.collection("rounds").document(l.id),
                mapOf("url" to l.getString("url"), "clue" to l.getString("clue").orEmpty(), "authorId" to null, "votes" to emptyMap<String, String>(), "reactions" to emptyMap<String, String>()),
            )
        }
        Tasks.await(batch.update(doc, mapOf("roundIds" to links.map { it.id }, "roundIndex" to 0)).commit())
    }

    fun reveal(code: String, round: Any?): String {
        val author = Tasks.await(group(code).collection("links").document(round as String).get()).getString("authorId")!!
        Tasks.await(round(code, round).update("authorId", author))
        return author
    }

    fun next(code: String, index: Int, total: Int) {
        Tasks.await(group(code).update(if (index + 1 < total) mapOf("roundIndex" to index + 1) else mapOf("status" to "finished")))
    }
}

/** Un selfie de mentira: degradado, cabeza, pelo y hombros, para ver el tratamiento a dos tonos. */
private fun fakeSelfie(width: Int, height: Int, skin: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), Color.rgb(250, 235, 210), Color.rgb(70, 90, 120), Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null
    val cx = width / 2f
    paint.color = Color.rgb(30, 40, 70)
    canvas.drawOval(cx - width * .45f, height * .72f, cx + width * .45f, height * 1.3f, paint)
    paint.color = skin
    canvas.drawCircle(cx, height * .42f, height * .26f, paint)
    paint.color = Color.rgb(40, 25, 20)
    canvas.drawArc(cx - height * .27f, height * .13f, cx + height * .27f, height * .6f, 180f, 180f, true, paint)
    paint.color = Color.BLACK
    canvas.drawCircle(cx - height * .09f, height * .42f, height * .025f, paint)
    canvas.drawCircle(cx + height * .09f, height * .42f, height * .025f, paint)
    return bitmap
}

@OptIn(ExperimentalTestApi::class)
class FullGameTest {
    @get:Rule val ui = createEmptyComposeRule()
    @get:Rule val name = org.junit.rules.TestName()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var vm: GameViewModel

    private fun s(id: Int, vararg args: Any) = context.getString(id, *args)

    @Before fun freshInstall() {
        context.getSharedPreferences("scrollparty", Context.MODE_PRIVATE).edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { vm = ViewModelProvider(it)[GameViewModel::class.java] }
    }

    private fun waitFor(text: String) {
        try {
            // Mientras otra actividad (ShareActivity) está delante no hay jerarquía: se sigue esperando.
            ui.waitUntil(TIMEOUT) { runCatching { ui.onAllNodes(hasText(text, substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        } catch (e: Throwable) {
            runCatching { shot("fail-" + name.methodName) } // lo que se veía cuando no apareció el texto
            throw e
        }
    }
    private fun waitForTag(tag: String) = ui.waitUntilAtLeastOneExists(hasTestTag(tag), TIMEOUT)
    private fun button(text: String) = hasClickAction() and hasText(text, ignoreCase = true)
    private fun click(text: String) { ui.waitUntilAtLeastOneExists(button(text), TIMEOUT); ui.onNode(button(text)).also { runCatching { it.performScrollTo() } }.performClick() }
    private fun textOf(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
    private fun count(text: String) = ui.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().size

    /** Guarda una captura de la pantalla actual para compararla con el prototipo. */
    private fun shot(name: String) {
        ui.waitForIdle()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, SHOTS)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        context.contentResolver.openOutputStream(uri)!!.use { ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun myLinkUrls(): List<Pair<String, String>> {
        var out = emptyList<Pair<String, String>>()
        scenario.onActivity { out = vm.myLinks.map { it.id to it.url } }
        return out
    }

    @Test fun partidaCompletaComoAnfitrion() {
        // Onboarding de 4 pasos.
        waitFor(s(R.string.onb1_title))
        shot("onboarding")
        repeat(3) { click(s(R.string.next)) }
        waitFor(s(R.string.onb4_title))
        click(s(R.string.get_started))

        // Selfie y nombre: la foto pasa por la misma ruta que la cámara (recorte, 256 px, JPEG, ≤ 40000).
        waitFor(s(R.string.profile_title))
        scenario.onActivity { vm.savePhoto(squareJpegBase64(fakeSelfie(1600, 900, Color.rgb(240, 200, 170)))) }
        assertTrue("foto demasiado grande: ${vm.photo!!.length}", vm.photo!!.length <= MAX_PHOTO_CHARS)
        waitFor(s(R.string.looks_good))
        ui.onNodeWithTag("name").performTextInput("Ana")
        shot("selfie")
        click(s(R.string.looks_good))

        // El anfitrión crea la fiesta.
        click(s(R.string.create_party))
        waitFor(s(R.string.add_reel))
        val code = context.getSharedPreferences("scrollparty", Context.MODE_PRIVATE).getString("code", null)!!

        val bob = Friend("bob", "Bob", 1, context, squareJpegBase64(fakeSelfie(900, 1200, Color.rgb(200, 150, 120))))
            .apply { join(code); addLink(code, "https://youtu.be/bob", CLUE) }
        val eva = Friend("eva", "Eva", 2, context).apply { join(code) }
        waitFor("Eva")

        // Añadir un reel con pista desde la propia app.
        click(s(R.string.add_reel))
        ui.onNodeWithTag("link").performTextInput("Mira https://www.tiktok.com/@ana/video/1 jaja")
        ui.onNodeWithTag("clue").performTextInput("seen at 3am")
        click(s(R.string.add_to_machine))
        waitFor("https://www.tiktok.com/@ana/video/1")
        waitFor("seen at 3am")

        // Compartir desde otra app: ShareActivity mete el enlace en la fiesta y se cierra.
        context.startActivity(
            Intent(context, ShareActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Look https://youtu.be/shared").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        waitFor("https://youtu.be/shared")
        waitFor(s(R.string.reels_sent))
        shot("lobby")

        click(s(R.string.start_party))
        waitFor(s(R.string.reel_of, 1, 3))
        val rounds = bob.roundIds(code)
        assertEquals(3, rounds.size)
        val mine = myLinkUrls()
        val tiktok = mine.first { it.second.contains("tiktok") }.first
        val hostUid = vm.uid!!
        var firstBobRound = true

        rounds.forEachIndexed { index, round ->
            waitFor(s(R.string.reel_of, index + 1, 3))
            if (round == bob.linkId) {
                // Reel de Bob: reacciones, pista, voto secreto del anfitrión (sin pistas de a quién) y reveal.
                eva.react(code, round, "laugh")
                if (firstBobRound) { Thread.sleep(450); shot("stage") }
                firstBobRound = false
                waitForTag("secretVote")
                ui.onNodeWithTag("secretVote").performClick()
                waitFor(s(R.string.secret_vote))
                shot("secret")
                ui.onNode(hasContentDescription(s(R.string.vote_for, "Bob"))).performClick()
                waitFor(s(R.string.voted_of, 1, 2))
                val afterBob = listOf(textOf(ui.onNodeWithTag("voteStatus")), textOf(ui.onNodeWithTag("secretVote")))
                ui.onNodeWithTag("secretVote").performClick()
                ui.onNode(hasContentDescription(s(R.string.vote_for, "Eva"))).performClick()
                ui.waitUntil(TIMEOUT) { count(s(R.string.vote_in)) > 0 }
                val afterEva = listOf(textOf(ui.onNodeWithTag("voteStatus")), textOf(ui.onNodeWithTag("secretVote")))
                assertEquals("elegir a otra persona no puede cambiar nada visible", afterBob, afterEva)
                assertTrue(afterBob[1].contains(s(R.string.vote_in), ignoreCase = true))
                assertEquals(0, count("Bob") + count("Eva"))
                ui.onNodeWithTag("secretVote").performClick()
                ui.onNode(hasContentDescription(s(R.string.vote_for, "Bob"))).performClick() // la respuesta buena

                eva.vote(code, round, hostUid)
                waitFor(s(R.string.voted_of, 2, 2))
                click(s(R.string.reveal_who))
                waitFor(s(R.string.author_reveal, "Bob"))
                waitFor(s(R.string.reveal_right))
                shot("reveal")
            } else {
                // Reel de Ana: no vota. Bob falla, Eva acierta.
                waitFor(s(R.string.this_ones_yours))
                assertEquals(0, ui.onAllNodes(hasTestTag("secretVote")).fetchSemanticsNodes().size)
                if (round == tiktok) {
                    bob.react(code, round, "laugh")
                    eva.react(code, round, "skull")
                } else {
                    eva.react(code, round, "fire")
                }
                bob.vote(code, round, eva.uid)
                eva.vote(code, round, hostUid)
                waitFor(s(R.string.voted_of, 2, 2))
                click(s(R.string.reveal_who))
                waitFor(s(R.string.author_reveal, "Ana"))
                waitFor(pluralMine(1))
            }
            click(s(if (index == 2) R.string.see_final else R.string.next_reel))
        }

        // Ana: 1 acierto + 2 engañados = 3. Bob: 1 engañado. Eva: 2 aciertos.
        waitFor(s(R.string.final_title))
        fun score(uid: String) = textOf(ui.onNodeWithTag("score-$uid", useUnmergedTree = true))
        assertEquals("3", score(hostUid)); assertEquals("1", score(bob.uid)); assertEquals("2", score(eva.uid))
        ui.onNode(hasContentDescription("${s(R.string.prize_detective)}: Eva")).assertExists()
        ui.onNode(hasContentDescription("${s(R.string.prize_bluffer)}: Ana")).assertExists()
        ui.onNode(hasContentDescription("${s(R.string.prize_reel)}: Ana")).assertExists() // 2 reacciones laugh+skull en su reel
        shot("final")
    }

    private fun pluralMine(n: Int) = context.resources.getQuantityString(R.plurals.reveal_mine, n, n)

    @Test fun partidaComoInvitadoConRuedaYPantallaDeAutor() {
        waitFor(s(R.string.onb1_title))
        click(s(R.string.skip))
        waitFor(s(R.string.profile_title))
        ui.onNodeWithTag("name").performTextInput("Iker")
        click(s(R.string.use_initials)) // sin foto: cara con iniciales

        val host = Friend("host", "Leo", 0, context, squareJpegBase64(fakeSelfie(1200, 900, Color.rgb(230, 190, 160))))
        val code = host.createGroup()
        waitForTag("code")
        ui.onNodeWithTag("code").performTextInput(code.lowercase())
        click(s(R.string.join_party))
        waitFor(s(R.string.waiting_host))

        val bob = Friend("bob2", "Bob", 1, context, squareJpegBase64(fakeSelfie(900, 1200, Color.rgb(200, 150, 120)))).apply { join(code); addLink(code, "https://youtu.be/bob", CLUE) }
        val eva = Friend("eva2", "Eva", 2, context).apply { join(code); addLink(code, "https://www.instagram.com/reel/eva/") }
        click(s(R.string.add_reel))
        ui.onNodeWithTag("link").performTextInput("https://youtu.be/iker")
        click(s(R.string.add_to_machine))
        waitFor("https://youtu.be/iker")
        waitFor(context.resources.getQuantityString(R.plurals.players, 4, 4))
        shot("guest-lobby")

        Thread.sleep(1500) // que el enlace de Iker llegue al servidor antes de barajar
        host.start(code)
        val rounds = host.roundIds(code)
        assertEquals(3, rounds.size)
        val myId = myLinkUrls().single().first
        var shotPick = true
        var shotYours = true

        rounds.forEachIndexed { index, round ->
            waitFor(if (round == myId) s(R.string.this_ones_yours) else s(R.string.who_sent_it))
            if (round == myId) {
                if (shotYours) { shot("yours"); shotYours = false }
                ui.onNode(hasContentDescription(s(R.string.react_skull))).performClick()
                bob.vote(code, round, host.uid)
            } else {
                // Rueda: tocar a Bob la centra y la selecciona; «That's my guess» la confirma y se puede cambiar.
                if (shotPick) { Thread.sleep(300); shot("pick"); shotPick = false }
                ui.onNode(hasText("Bob") and hasClickAction()).performClick()
                click(s(R.string.my_guess))
                waitFor(s(R.string.guess_locked, "Bob"))
                ui.onNode(hasContentDescription(s(R.string.react_fire))).performClick()
                bob.vote(code, round, host.uid)
            }
            val author = host.reveal(code, round)
            val name = mapOf(host.uid to "Leo", bob.uid to "Bob", eva.uid to "Eva").getOrDefault(author, "Iker")
            waitFor(s(R.string.author_reveal, name))
            waitFor(s(R.string.waiting_next))
            if (index == 0) shot("guest-reveal")
            host.next(code, index, 3)
        }
        waitFor(s(R.string.final_title))
        shot("guest-final")
    }
}
