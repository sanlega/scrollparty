package com.santilega.scrollparty

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
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

private var emulatorConfigured = false // useEmulator solo se puede llamar una vez por proceso
private const val EMULATOR_HOST = "10.0.2.2" // el PC visto desde el emulador de Android

/** Error con texto localizado para mostrar tal cual. */
class UiError(@StringRes val res: Int, vararg val args: Any) : Exception()

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

    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false)); private set
    var name by mutableStateOf(prefs.getString("name", "") ?: ""); private set
    var photo by mutableStateOf(prefs.getString("photo", null)); private set

    /** Hay una fiesta guardada en este móvil (se está recuperando o ya cargó). */
    val hasSavedParty get() = prefs.getString("code", null) != null
    /** Terminó el arranque: sesión iniciada y fiesta guardada recuperada (o no había). */
    var ready by mutableStateOf(false); private set

    val isAdmin get() = group?.adminId == uid
    val me get() = members.find { it.id == uid }
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
        if (BuildConfig.USE_EMULATOR && !emulatorConfigured) {
            emulatorConfigured = true
            auth.useEmulator(EMULATOR_HOST, 9099)
            db.useEmulator(EMULATOR_HOST, 8180)
        }
        run {
            try {
                val user = auth.currentUser ?: auth.signInAnonymously().await().user!!
                uid = user.uid
                prefs.getString("code", null)?.let { join(it) }
            } finally {
                ready = true
            }
        }
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                error = null
                block()
            } catch (e: UiError) {
                error = getApplication<Application>().getString(e.res, *e.args)
            } catch (e: Exception) {
                error = e.localizedMessage ?: getApplication<Application>().getString(R.string.error_generic)
            }
        }
    }

    private fun groupDoc(code: String = group!!.code) = db.collection("groups").document(code)

    fun finishOnboarding() {
        prefs.edit().putBoolean("onboarded", true).apply()
        onboarded = true
    }

    fun saveName(name: String) {
        this.name = name.trim().take(30)
        prefs.edit().putString("name", this.name).apply()
    }

    fun savePhoto(photo: String?) {
        this.photo = photo
        prefs.edit().putString("photo", photo).apply()
    }

    fun create() = run {
        val code = generateSequence { newCode() }.first { !groupDoc(it).get().await().exists() }
        groupDoc(code).set(
            mapOf("adminId" to uid, "status" to "collecting", "roundIds" to emptyList<String>(), "roundIndex" to 0),
        ).await()
        join(code)
    }

    fun joinGroup(code: String) = run { join(code.trim().uppercase()) }

    private suspend fun join(code: String) {
        val doc = groupDoc(code)
        if (!doc.get().await().exists()) {
            forget()
            throw UiError(R.string.error_no_group, code)
        }
        val me = doc.collection("members").document(uid!!)
        if (!me.get().await().exists()) {
            val data = mutableMapOf<String, Any>(
                "name" to name, "emoji" to "", "color" to freeColor(emptyList()), "count" to 0, "joinedAt" to System.currentTimeMillis(),
            )
            photo?.let { data["photo"] = it }
            me.set(data).await()
        }
        prefs.edit().putString("code", code).apply()
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
        publishPartyShortcut(getApplication(), code)
        listeners += doc.collection("members").addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener onError(e)
            members = snap!!.documents.map {
                Member(
                    it.id,
                    it.getString("name").orEmpty(),
                    it.getLong("color")?.toInt() ?: 0,
                    it.getString("photo"),
                    it.getLong("count")?.toInt() ?: 0,
                    it.getLong("joinedAt") ?: 0,
                )
            }.sortedBy { it.joinedAt }
            resolveColorClash()
        }
        listeners += doc.collection("links").whereEqualTo("authorId", uid).addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener onError(e)
            myLinks = snap!!.documents.map { Link(it.id, it.getString("url").orEmpty(), it.getString("clue").orEmpty()) }
        }
        listeners += doc.collection("rounds").addSnapshotListener { snap, e ->
            if (e != null) return@addSnapshotListener onError(e)
            rounds = snap!!.documents.map { d ->
                @Suppress("UNCHECKED_CAST")
                Round(
                    d.id,
                    d.getString("url").orEmpty(),
                    d.getString("authorId"),
                    d.get("votes") as? Map<String, String> ?: emptyMap(),
                    d.get("reactions") as? Map<String, String> ?: emptyMap(),
                    d.getString("clue").orEmpty(),
                )
            }
        }
    }

    /**
     * Las reglas no dejan listar los miembros antes de serlo, así que el color se elige al azar al entrar;
     * si coincide con el de alguien que entró antes, se cambia por uno libre.
     */
    private fun resolveColorClash() {
        val mine = me ?: return
        val earlier = members.takeWhile { it.id != mine.id }.map { it.color }
        if (mine.color !in earlier) return
        val taken = members.filter { it.id != mine.id }.map { it.color }
        groupDoc().collection("members").document(mine.id).update("color", freeColor(taken))
    }

    /** Solo olvida el grupo en este móvil; salir de verdad (borrar el miembro) está fuera de alcance. */
    fun forget() {
        listeners.forEach { it.remove() }
        listeners.clear()
        prefs.edit().remove("code").apply()
        removePartyShortcut(getApplication())
        group = null
        members = emptyList()
        myLinks = emptyList()
        rounds = emptyList()
    }

    /** Suspende hasta que el enlace está guardado: lo usa también ShareActivity, que se cierra justo después. */
    suspend fun addLinkNow(text: String, clue: String) {
        val url = extractLink(text) ?: throw UiError(R.string.error_link)
        val doc = groupDoc()
        val data = mutableMapOf<String, Any?>("url" to url, "authorId" to uid)
        clue.trim().take(MAX_CLUE).takeIf { it.isNotEmpty() }?.let { data["clue"] = it }
        db.batch()
            .set(doc.collection("links").document(), data)
            .update(doc.collection("members").document(uid!!), "count", FieldValue.increment(1))
            .commit().await()
    }

    fun addLink(text: String, clue: String, onDone: () -> Unit = {}) = run { addLinkNow(text, clue); onDone() }

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
                mapOf(
                    "url" to link.getString("url"),
                    "clue" to link.getString("clue").orEmpty(),
                    "authorId" to null,
                    "votes" to emptyMap<String, String>(),
                    "reactions" to emptyMap<String, String>(),
                ),
            )
        }
        batch.update(doc, mapOf("roundIds" to links.map { it.id }, "roundIndex" to 0)).commit().await()
    }

    fun vote(round: Round, guess: String) = run {
        groupDoc().collection("rounds").document(round.id).update("votes.$uid", guess).await()
    }

    fun react(round: Round, reaction: String) = run {
        groupDoc().collection("rounds").document(round.id).update("reactions.$uid", reaction).await()
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
