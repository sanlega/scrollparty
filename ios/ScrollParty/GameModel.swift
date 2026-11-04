import FirebaseAuth
import FirebaseCore
import FirebaseFirestore
import Foundation
import Observation

private func infoValue(_ key: String) -> String {
    Bundle.main.object(forInfoDictionaryKey: key) as? String ?? ""
}

private func appError(_ message: String) -> NSError {
    NSError(domain: "ScrollParty", code: 1, userInfo: [NSLocalizedDescriptionKey: message])
}

@MainActor
@Observable
final class GameModel {
    var uid: String?
    var group: Party?
    var members: [Member] = []
    var myLinks: [Link] = []
    var rounds: [Round] = []
    var toast: ToastMessage?
    /// Perfil de este móvil: nombre, color preferido (0-11) y selfie en base64.
    var name: String
    var color: Int
    var photo: String?
    @ObservationIgnored private(set) var isDemo = false

    @ObservationIgnored private let defaults = UserDefaults.standard
    @ObservationIgnored private var db: Firestore!
    @ObservationIgnored private var auth: Auth!
    @ObservationIgnored private var listeners: [ListenerRegistration] = []

    var isAdmin: Bool { group?.adminId == uid }
    var currentRound: Round? {
        guard let group, group.roundIndex < group.roundIds.count else { return nil }
        return rounds.first { $0.id == group.roundIds[group.roundIndex] }
    }
    /// Las rondas en el orden en que se juegan.
    var playedRounds: [Round] {
        guard let group else { return [] }
        return group.roundIds.compactMap { id in rounds.first { $0.id == id } }
    }

    #if DEBUG
    /// Modelo de demostración: sin Firebase ni red. Lo rellena `GameModel.demo(_:)`.
    init(demo: Bool) {
        isDemo = true
        name = "Iker"
        color = 5
        photo = nil
    }
    #endif

    init() {
        let saved = UserDefaults.standard
        name = saved.string(forKey: "name") ?? ""
        color = saved.object(forKey: "color") as? Int ?? Int.random(in: 0..<playerColorCount)
        photo = saved.string(forKey: "photo")
        let apiKey = infoValue("FirebaseAPIKey")
        let useEmulator = apiKey.isEmpty
        if FirebaseApp.app() == nil {
            let options = FirebaseOptions(
                googleAppID: useEmulator ? "1:1234567890:ios:0123456789abcdef" : infoValue("FirebaseAppID"),
                gcmSenderID: useEmulator ? "1234567890" : infoValue("FirebaseSenderID")
            )
            options.apiKey = useEmulator ? "demo-key" : apiKey
            options.projectID = useEmulator ? "demo-scrollparty" : infoValue("FirebaseProjectID")
            FirebaseApp.configure(options: options)
        }
        db = Firestore.firestore()
        auth = Auth.auth()
        if useEmulator {
            auth.useEmulator(withHost: "127.0.0.1", port: 9099)
            let settings = db.settings
            settings.host = "127.0.0.1:8180"
            settings.isSSLEnabled = false
            db.settings = settings
        }
        perform { [self] in
            let user: User
            if let current = auth.currentUser {
                user = current
            } else {
                user = try await auth.signInAnonymously().user
            }
            uid = user.uid
            if let code = defaults.string(forKey: "code") {
                try await join(code: code)
            }
        }
    }

    func showError(_ message: String) {
        toast = ToastMessage(title: message)
    }

    private func perform(_ block: @escaping @MainActor () async throws -> Void) {
        guard !isDemo else { return }
        Task { @MainActor in
            do {
                toast = nil
                try await block()
            } catch {
                showError(error.localizedDescription)
            }
        }
    }

    private func groupDoc(_ code: String? = nil) -> DocumentReference {
        db.collection("groups").document(code ?? group!.code)
    }

    func saveProfile(name: String, photo: String?) {
        self.name = String(name.trimmingCharacters(in: .whitespaces).prefix(30))
        self.photo = photo
        defaults.set(self.name, forKey: "name")
        defaults.set(color, forKey: "color")
        if let photo { defaults.set(photo, forKey: "photo") } else { defaults.removeObject(forKey: "photo") }
    }

    func create() {
        perform { [self] in
            var code = newCode()
            while try await groupDoc(code).getDocument().exists { code = newCode() }
            try await groupDoc(code).setData([
                "adminId": uid!, "status": "collecting", "roundIds": [String](), "roundIndex": 0,
            ])
            try await join(code: code)
        }
    }

    func joinGroup(code: String) {
        perform { [self] in
            try await join(code: code.trimmingCharacters(in: .whitespaces).uppercased())
        }
    }

    private func parseMember(_ d: DocumentSnapshot) -> Member {
        let data = d.data() ?? [:]
        return Member(
            id: d.documentID,
            name: data["name"] as? String ?? "",
            color: data["color"] as? Int ?? 0,
            photo: data["photo"] as? String,
            count: data["count"] as? Int ?? 0,
            joinedAt: data["joinedAt"] as? Int64 ?? 0
        )
    }

    private func join(code: String) async throws {
        let doc = groupDoc(code)
        guard try await doc.getDocument().exists else {
            forget()
            throw appError(String(localized: "No party with code \(code)"))
        }
        let me = doc.collection("members").document(uid!)
        if try await !me.getDocument().exists {
            var data: [String: Any] = [
                "name": String(name.prefix(30)), "emoji": "", "color": color, "count": 0,
                "joinedAt": Int64(Date().timeIntervalSince1970 * 1000),
            ]
            if let photo { data["photo"] = photo }
            try await me.setData(data)
        }
        // El color es único en el grupo: si otro que entró antes lo tiene, me toca el primero libre.
        let all = try await doc.collection("members").getDocuments().documents.map(parseMember)
        if let mine = all.first(where: { $0.id == uid }) {
            let final = assignColor(me: mine, preferred: mine.color, members: all)
            if final != mine.color { try await me.updateData(["color": final]) }
        }
        defaults.set(code, forKey: "code")
        shareWithExtension(code: code)
        listen(code)
    }

    /// La extensión de compartir no lleva Firebase: necesita estos datos para hablar con Firestore por REST.
    private func shareWithExtension(code: String) {
        guard let user = auth.currentUser, let token = user.refreshToken else { return }
        SharedStore.save(SharedParty(
            code: code, refreshToken: token,
            apiKey: infoValue("FirebaseAPIKey"), projectId: infoValue("FirebaseProjectID"), uid: user.uid
        ))
    }

    private func listen(_ code: String) {
        listeners.forEach { $0.remove() }
        listeners = []
        let doc = groupDoc(code)
        listeners.append(doc.addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let error { self.showError(error.localizedDescription); return }
            guard let snap, snap.exists, let data = snap.data() else { self.forget(); return }
            self.group = Party(
                code: code,
                adminId: data["adminId"] as? String ?? "",
                status: data["status"] as? String ?? "",
                roundIds: data["roundIds"] as? [String] ?? [],
                roundIndex: data["roundIndex"] as? Int ?? 0
            )
        })
        listeners.append(doc.collection("members").addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let error { self.showError(error.localizedDescription); return }
            self.members = (snap?.documents ?? []).map { self.parseMember($0) }.sorted { $0.joinedAt < $1.joinedAt }
        })
        listeners.append(doc.collection("links").whereField("authorId", isEqualTo: uid!).addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let error { self.showError(error.localizedDescription); return }
            self.myLinks = (snap?.documents ?? []).map { Link(id: $0.documentID, url: $0.data()["url"] as? String ?? "") }
        })
        listeners.append(doc.collection("rounds").addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let error { self.showError(error.localizedDescription); return }
            self.rounds = (snap?.documents ?? []).map { d in
                let data = d.data()
                return Round(
                    id: d.documentID,
                    url: data["url"] as? String ?? "",
                    authorId: data["authorId"] as? String,
                    votes: data["votes"] as? [String: String] ?? [:],
                    clue: data["clue"] as? String ?? "",
                    reactions: data["reactions"] as? [String: String] ?? [:]
                )
            }
        })
    }

    /// shortcut: solo olvida el grupo en este móvil y el miembro sigue contando; salida real en el ticket 005.
    func forget() {
        listeners.forEach { $0.remove() }
        listeners = []
        defaults.removeObject(forKey: "code")
        SharedStore.clear()
        group = nil
        members = []
        myLinks = []
        rounds = []
    }

    func addLink(_ text: String, clue: String) {
        perform { [self] in
            guard let url = extractLink(text) else {
                throw appError(String(localized: "Paste an Instagram, TikTok or YouTube link"))
            }
            let doc = groupDoc()
            let batch = db.batch()
            var data: [String: Any] = ["url": url, "authorId": uid!]
            let trimmed = String(clue.trimmingCharacters(in: .whitespacesAndNewlines).prefix(80))
            if !trimmed.isEmpty { data["clue"] = trimmed }
            batch.setData(data, forDocument: doc.collection("links").document())
            batch.updateData(["count": FieldValue.increment(Int64(1))], forDocument: doc.collection("members").document(uid!))
            try await batch.commit()
            toast = ToastMessage(title: String(localized: "In the machine ✓"))
        }
    }

    func removeLink(_ link: Link) {
        perform { [self] in
            let doc = groupDoc()
            let batch = db.batch()
            batch.deleteDocument(doc.collection("links").document(link.id))
            batch.updateData(["count": FieldValue.increment(Int64(-1))], forDocument: doc.collection("members").document(uid!))
            try await batch.commit()
        }
    }

    /// Idempotente: si falla a medias, el anfitrión puede volver a lanzarlo.
    func start() {
        perform { [self] in
            let doc = groupDoc()
            try await doc.updateData(["status": "playing"])
            let links = try await doc.collection("links").getDocuments().documents.shuffled()
            let batch = db.batch()
            for link in links {
                batch.setData(
                    [
                        "url": link.data()["url"] as? String ?? "",
                        "clue": link.data()["clue"] as? String ?? "",
                        "authorId": NSNull(),
                        "votes": [String: String](),
                        "reactions": [String: String](),
                    ],
                    forDocument: doc.collection("rounds").document(link.documentID)
                )
            }
            batch.updateData(["roundIds": links.map(\.documentID), "roundIndex": 0], forDocument: doc)
            try await batch.commit()
        }
    }

    func vote(_ round: Round, guess: String) {
        perform { [self] in
            try await groupDoc().collection("rounds").document(round.id).updateData(["votes.\(uid!)": guess])
        }
    }

    func react(_ round: Round, _ reaction: Reaction) {
        perform { [self] in
            try await groupDoc().collection("rounds").document(round.id).updateData(["reactions.\(uid!)": reaction.rawValue])
        }
    }

    func reveal(_ round: Round) {
        perform { [self] in
            let doc = groupDoc()
            let link = try await doc.collection("links").document(round.id).getDocument()
            try await doc.collection("rounds").document(round.id).updateData(["authorId": link.data()?["authorId"] ?? NSNull()])
        }
    }

    func next() {
        perform { [self] in
            guard let group else { return }
            if group.roundIndex + 1 < group.roundIds.count {
                try await groupDoc().updateData(["roundIndex": group.roundIndex + 1])
            } else {
                try await groupDoc().updateData(["status": "finished"])
            }
        }
    }
}
