package com.santilega.scrollparty

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val TIMEOUT = 20_000L

/** Otro móvil: un cliente Firebase con su propia sesión anónima, sujeto a las mismas reglas. */
private class Friend(tag: String, private val name: String, context: Context) {
    private val app = FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder().setApiKey("demo-key").setProjectId("demo-scrollparty").setApplicationId("1:1:android:1").build(),
        tag,
    )
    private val db = FirebaseFirestore.getInstance(app).apply { useEmulator("10.0.2.2", 8180) }
    val uid: String = FirebaseAuth.getInstance(app).run {
        useEmulator("10.0.2.2", 9099)
        Tasks.await(signInAnonymously()).user!!.uid
    }
    var linkId = ""

    private fun group(code: String) = db.collection("groups").document(code)

    fun join(code: String) {
        val data = mapOf("name" to name, "emoji" to "🤖", "count" to 0, "joinedAt" to System.currentTimeMillis())
        Tasks.await(group(code).collection("members").document(uid).set(data))
    }

    fun addLink(code: String, url: String) {
        val doc = group(code)
        val link = doc.collection("links").document()
        linkId = link.id
        Tasks.await(
            db.batch()
                .set(link, mapOf("url" to url, "authorId" to uid))
                .update(doc.collection("members").document(uid), "count", FieldValue.increment(1))
                .commit(),
        )
    }

    fun roundIds(code: String) = Tasks.await(group(code).get()).get("roundIds") as List<*>
    fun vote(code: String, round: Any?, guess: String): DocumentReference {
        val doc = group(code).collection("rounds").document(round as String)
        Tasks.await(doc.update("votes.$uid", guess))
        return doc
    }
}

@OptIn(ExperimentalTestApi::class)
class FullGameTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun waitFor(text: String) = ui.waitUntilAtLeastOneExists(hasText(text, substring = true), TIMEOUT)

    @Test fun partidaCompleta() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // El anfitrión crea el grupo desde la UI.
        waitFor("Create group")
        ui.onNodeWithText("Your name").performTextInput("Ana")
        ui.onNodeWithText("Create group").performClick()
        waitFor("Add to the bag")
        val prefs = context.getSharedPreferences("scrollparty", Context.MODE_PRIVATE)
        val code = prefs.getString("code", null)!!

        val bob = Friend("bob", "Bob", context).apply { join(code); addLink(code, "https://youtu.be/bob") }
        val eva = Friend("eva", "Eva", context).apply { join(code) }
        waitFor("Eva")

        ui.onNodeWithText("Paste a reel, TikTok or Short").performTextInput("Mira https://www.tiktok.com/@ana/video/1 jaja")
        ui.onNodeWithText("Add to the bag").performClick()
        waitFor("https://www.tiktok.com/@ana/video/1")
        ui.onNodeWithText("Start the party").performClick()
        waitFor("Who sent this?")

        val rounds = bob.roundIds(code)
        assertEquals(2, rounds.size)
        val hostUid = FirebaseAuth.getInstance().currentUser!!.uid

        rounds.forEachIndexed { index, round ->
            waitFor("Reel ${index + 1} of 2")
            if (round == bob.linkId) {
                // Reel de Bob: Ana acierta desde la UI y Eva falla.
                waitFor("Who sent this?")
                ui.onNodeWithText("🤖  Bob").performClick()
                waitFor("1/2 have voted")
                eva.vote(code, round, hostUid)
                waitFor("It was 🤖 Bob!")
                waitFor("You got it! +1")
            } else {
                // Reel de Ana: no ve botones de voto. Bob falla y Eva acierta.
                waitFor("This one's yours")
                bob.vote(code, round, eva.uid)
                eva.vote(code, round, hostUid)
                waitFor("It was 😎 Ana!")
            }
            ui.onNodeWithText("Next").performClick()
        }

        waitFor("Final scores")
        // Ana: 1 acierto + 1 engañado. Bob: 1 engañado. Eva: 1 acierto.
        assertTrue(ui.onAllNodes(hasText("1. 😎 Ana — 2")).fetchSemanticsNodes().size == 1)
        waitFor("🕵️ Detective: 😎 Ana, 🤖 Eva")
        waitFor("🥷 Infiltrator: 😎 Ana, 🤖 Bob")
    }
}
