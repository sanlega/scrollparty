import FirebaseAuth
import FirebaseCore
import FirebaseFirestore
import Foundation
import Observation

private func infoValue(_ key: String) -> String {
    Bundle.main.object(forInfoDictionaryKey: key) as? String ?? ""
}

@MainActor
@Observable
final class GameModel {
    var uid: String?
    var group: Group?
    var members: [Member] = []
    var myLinks: [Link] = []
    var rounds: [Round] = []
    var error: String?

    @ObservationIgnored private let defaults = UserDefaults.standard
    @ObservationIgnored private let db: Firestore
    @ObservationIgnored private let auth: Auth
    @ObservationIgnored private var listeners: [ListenerRegistration] = []

    var name: String { defaults.string(forKey: "name") ?? "" }
    var emoji: String { defaults.string(forKey: "emoji") ?? "😎" }
    var isAdmin: Bool { group?.adminId == uid }
    var currentRound: Round? {
        guard let group, group.roundIndex < group.roundIds.count else { return nil }
        return rounds.first { $0.id == group.roundIds[group.roundIndex] }
    }

    init() {
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
                try await join(code: code, name: name, emoji: emoji)
            }
        }
    }

    private func perform(_ block: @escaping @MainActor () async throws -> Void) {
        Task { @MainActor in
            do {
                error = nil
                try await block()
            } catch {
                self.error = error.localizedDescription
            }
        }
    }

    private func groupDoc(_ code: String? = nil) -> DocumentReference {
        db.collection("groups").document(code ?? group!.code)
    }

    func create(name: String, emoji: String) {
        perform { [self] in
            var code = newCode()
            while try await groupDoc(code).getDocument().exists { code = newCode() }
            try await groupDoc(code).setData([
                "adminId": uid!, "status": "collecting", "roundIds": [String](), "roundIndex": 0,
            ])
            try await join(code: code, name: name, emoji: emoji)
        }
    }

    func joinGroup(code: String, name: String, emoji: String) {
        perform { [self] in
            try await join(code: code.trimmingCharacters(in: .whitespaces).uppercased(), name: name, emoji: emoji)
        }
    }

    private func join(code: String, name: String, emoji: String) async throws {
        let doc = groupDoc(code)
        guard try await doc.getDocument().exists else {
            forget()
            throw NSError(domain: "ScrollParty", code: 1, userInfo: [NSLocalizedDescriptionKey: "No group with code \(code)"])
        }
        let me = doc.collection("members").document(uid!)
        if try await !me.getDocument().exists {
            try await me.setData([
                "name": String(name.prefix(30)), "emoji": emoji, "count": 0,
                "joinedAt": Int64(Date().timeIntervalSince1970 * 1000),
            ])
        }
        defaults.set(code, forKey: "code")
        defaults.set(name, forKey: "name")
        defaults.set(emoji, forKey: "emoji")
        listen(code)
    }

    private func listen(_ code: String) {
        let doc = groupDoc(code)
        listeners.append(doc.addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let error { self.error = error.localizedDescription; return }
            guard let snap, snap.exists, let data = snap.data() else { self.forget(); return }
            self.group = Group(
                code: code,
                adminId: data["adminId"] as? String ?? "",
                status: data["status"] as? String ?? "",
                roundIds: data["roundIds"] as? [String] ?? [],
                roundIndex: data["roundIndex"] as? Int ?? 0
            )
        })
        listeners.append(doc.collection("members").addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let error { self.error = error.localizedDescription; return }
            self.members = (snap?.documents ?? []).map { d in
                let data = d.data()
                return Member(
                    id: d.documentID,
                    name: data["name"] as? String ?? "",
                    emoji: data["emoji"] as? String ?? "",
                    count: data["count"] as? Int ?? 0,
                    joinedAt: data["joinedAt"] as? Int64 ?? 0
                )
            }.sorted { $0.joinedAt < $1.joinedAt }
        })
        listeners.append(doc.collection("links").whereField("authorId", isEqualTo: uid!).addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let error { self.error = error.localizedDescription; return }
            self.myLinks = (snap?.documents ?? []).map { Link(id: $0.documentID, url: $0.data()["url"] as? String ?? "") }
        })
        listeners.append(doc.collection("rounds").addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let error { self.error = error.localizedDescription; return }
            self.rounds = (snap?.documents ?? []).map { d in
                let data = d.data()
                return Round(
                    id: d.documentID,
                    url: data["url"] as? String ?? "",
                    authorId: data["authorId"] as? String,
                    votes: data["votes"] as? [String: String] ?? [:]
                )
            }
        })
    }

    /// shortcut: solo olvida el grupo en este móvil y el miembro sigue contando; salida real en el ticket 005.
    func forget() {
        listeners.forEach { $0.remove() }
        listeners = []
        defaults.removeObject(forKey: "code")
        group = nil
        members = []
        myLinks = []
        rounds = []
    }

    func addLink(_ text: String) {
        perform { [self] in
            guard let url = extractLink(text) else {
                throw NSError(domain: "ScrollParty", code: 2, userInfo: [NSLocalizedDescriptionKey: "Paste an Instagram, TikTok or YouTube link"])
            }
            let doc = groupDoc()
            let batch = db.batch()
            batch.setData(["url": url, "authorId": uid!], forDocument: doc.collection("links").document())
            batch.updateData(["count": FieldValue.increment(Int64(1))], forDocument: doc.collection("members").document(uid!))
            try await batch.commit()
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
                    ["url": link.data()["url"] as? String ?? "", "authorId": NSNull(), "votes": [String: String]()],
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
