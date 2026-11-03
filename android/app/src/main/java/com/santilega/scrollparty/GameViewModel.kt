package com.santilega.scrollparty

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val EMULATOR_HOST = "10.0.2.2" // el PC visto desde el emulador de Android

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("scrollparty", Context.MODE_PRIVATE)
    private val db: FirebaseFirestore
    private val auth: FirebaseAuth
    private val listeners = mutableListOf<ListenerRegistration>()

    var uid by mutableStateOf<String?>(null); private set
    var group by mutableStateOf<Group?>(null); private set
    var members by mutableStateOf(emptyList<Member>()); private set
    var myLinks by mutableStateOf(emptyList<Link>()); private set
    var rounds by mutableStateOf(emptyList<Round>()); private set
    var error by mutableStateOf<String?>(null)

    val name get() = prefs.getString("name", "") ?: ""
    val emoji get() = prefs.getString("emoji", "😎") ?: "😎"
    val isAdmin get() = group?.adminId == uid
    val currentRound get() = group?.let { g -> rounds.find { it.id == g.roundIds.getOrNull(g.roundIndex) } }

    init {
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(
                app,
                FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .build(),
            )
        }
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (BuildConfig.USE_EMULATOR) {
            auth.useEmulator(EMULATOR_HOST, 9099)
            db.useEmulator(EMULATOR_HOST, 8180)
        }
        run {
            val user = auth.currentUser ?: auth.signInAnonymously().await().user!!
            uid = user.uid
            prefs.getString("code", null)?.let { join(it, name, emoji) }
        }
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                error = null
                block()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Something went wrong"
            }
        }
    }

    private fun groupDoc(code: String = group!!.code) = db.collection("groups").document(code)

    fun create(name: String, emoji: String) = run {
        val code = generateSequence { newCode() }.first { !groupDoc(it).get().await().exists() }
        groupDoc(code).set(
            mapOf("adminId" to uid, "status" to "collecting", "roundIds" to emptyList<String>(), "roundIndex" to 0),
        ).await()
        join(code, name, emoji)
    }

    fun joinGroup(code: String, name: String, emoji: String) = run { join(code.trim().uppercase(), name, emoji) }

    private suspend fun join(code: String, name: String, emoji: String) {
        val doc = groupDoc(code)
        if (!doc.get().await().exists()) {
            forget()
            throw IllegalStateException("No group with code $code")
        }
        val me = doc.collection("members").document(uid!!)
        if (!me.get().await().exists()) {
            me.set(mapOf("name" to name.take(30), "emoji" to emoji, "count" to 0, "joinedAt" to System.currentTimeMillis())).await()
        }
        prefs.edit().putString("code", code).putString("name", name).putString("emoji", emoji).apply()
        listen(code)
    }

    private fun listen(code: String) {
        val doc = groupDoc(code)
        val onError = { e: Exception -> error = e.localizedMessage }
        listeners += doc.addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener onError(e)
            if (snap == null || !snap.exists()) return@addSnapshotListener forget()
            group = Group(
                code,
                snap.getString("adminId").orEmpty(),
                snap.getString("status").orEmpty(),
                (snap.get("roundIds") as? List<*>).orEmpty().filterIsInstance<String>(),
                snap.getLong("roundIndex")?.toInt() ?: 0,
            )
        }
        listeners += doc.collection("members").addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener onError(e)
            members = snap!!.documents.map {
                Member(
                    it.id,
                    it.getString("name").orEmpty(),
                    it.getString("emoji").orEmpty(),
                    it.getLong("count")?.toInt() ?: 0,
                    it.getLong("joinedAt") ?: 0,
                )
            }.sortedBy { it.joinedAt }
        }
        listeners += doc.collection("links").whereEqualTo("authorId", uid).addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener onError(e)
            myLinks = snap!!.documents.map { Link(it.id, it.getString("url").orEmpty()) }
        }
        listeners += doc.collection("rounds").addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener onError(e)
            rounds = snap!!.documents.map { d ->
                @Suppress("UNCHECKED_CAST")
                Round(d.id, d.getString("url").orEmpty(), d.getString("authorId"), d.get("votes") as? Map<String, String> ?: emptyMap())
            }
        }
    }

    /** shortcut: solo olvida el grupo en este móvil y el miembro sigue contando; salida real en el ticket 005. */
    fun forget() {
        listeners.forEach { it.remove() }
        listeners.clear()
        prefs.edit().remove("code").apply()
        group = null
        members = emptyList()
        myLinks = emptyList()
        rounds = emptyList()
    }

    fun addLink(text: String) = run {
        val url = extractLink(text) ?: throw IllegalArgumentException("Paste an Instagram, TikTok or YouTube link")
        val doc = groupDoc()
        db.batch()
            .set(doc.collection("links").document(), mapOf("url" to url, "authorId" to uid))
            .update(doc.collection("members").document(uid!!), "count", FieldValue.increment(1))
            .commit().await()
    }

    fun removeLink(link: Link) = run {
        val doc = groupDoc()
        db.batch()
            .delete(doc.collection("links").document(link.id))
            .update(doc.collection("members").document(uid!!), "count", FieldValue.increment(-1))
            .commit().await()
    }

    /** Idempotente: si falla a medias, el anfitrión puede volver a lanzarlo. */
    fun start() = run {
        val doc = groupDoc()
        doc.update("status", "playing").await()
        val links = doc.collection("links").get().await().documents.shuffled()
        val batch = db.batch()
        for (link in links) {
            batch.set(
                doc.collection("rounds").document(link.id),
                mapOf("url" to link.getString("url"), "authorId" to null, "votes" to emptyMap<String, String>()),
            )
        }
        batch.update(doc, mapOf("roundIds" to links.map { it.id }, "roundIndex" to 0)).commit().await()
    }

    fun vote(round: Round, guess: String) = run {
        groupDoc().collection("rounds").document(round.id).update("votes.$uid", guess).await()
    }

    fun reveal(round: Round) = run {
        val doc = groupDoc()
        val author = doc.collection("links").document(round.id).get().await().getString("authorId")
        doc.collection("rounds").document(round.id).update("authorId", author).await()
    }

    fun next() = run {
        val g = group!!
        if (g.roundIndex + 1 < g.roundIds.size) groupDoc().update("roundIndex", g.roundIndex + 1).await()
        else groupDoc().update("status", "finished").await()
    }
}
