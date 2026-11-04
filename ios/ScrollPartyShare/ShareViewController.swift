import SwiftUI
import UIKit
import UniformTypeIdentifiers

/// Resultado visible en el panel.
enum ShareState: Equatable {
    case working
    case done(String)
    case failed(String)
}

/// Sin SDK de Firebase: el token se renueva y el enlace se escribe por la API REST, en un único `commit` atómico.
@MainActor
final class ShareModel: ObservableObject {
    @Published var state = ShareState.working
    private weak var context: NSExtensionContext?

    init(context: NSExtensionContext?) {
        self.context = context
    }

    func close() {
        context?.completeRequest(returningItems: nil)
    }

    func run() async {
        do {
            let code = try await addSharedLink()
            state = .done(code)
            try? await Task.sleep(nanoseconds: 1_600_000_000)
            close()
        } catch let error as ShareFailure {
            state = .failed(error.message)
        } catch {
            state = .failed(String(localized: "Couldn't reach the party. Check your connection and try again."))
        }
    }

    // MARK: - Texto compartido

    private func sharedTexts() async -> [String] {
        var texts: [String] = []
        let items = context?.inputItems.compactMap { $0 as? NSExtensionItem } ?? []
        for item in items {
            if let text = item.attributedContentText?.string { texts.append(text) }
            for provider in item.attachments ?? [] {
                for type in [UTType.url, UTType.plainText] where provider.hasItemConformingToTypeIdentifier(type.identifier) {
                    guard let value = try? await provider.loadItem(forTypeIdentifier: type.identifier) else { continue }
                    if let url = value as? URL {
                        texts.append(url.absoluteString)
                    } else if let string = value as? String {
                        texts.append(string)
                    }
                }
            }
        }
        return texts
    }

    // MARK: - REST

    private func addSharedLink() async throws -> String {
        guard let url = await sharedTexts().compactMap({ extractLink($0) }).first else {
            throw ShareFailure(String(localized: "That isn't an Instagram, TikTok or YouTube link."))
        }
        guard let party = SharedStore.load() else {
            throw ShareFailure(String(localized: "Open ScrollParty and join a party first."))
        }
        guard !party.apiKey.isEmpty, !party.projectId.isEmpty else {
            throw ShareFailure(String(localized: "Sharing isn't available in this build."))
        }
        let token = try await idToken(party)
        let base = "projects/\(party.projectId)/databases/(default)/documents"
        let status = try await partyStatus(party, base: base, token: token)
        guard status == "collecting" else {
            throw ShareFailure(String(localized: "The party has already started."))
        }
        let linkId = String((0..<20).map { _ in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".randomElement()! })
        let linkFields: JSON = [
            "url": ["stringValue": url] as JSON,
            "authorId": ["stringValue": party.uid] as JSON,
        ]
        let linkWrite: JSON = [
            "update": ["name": "\(base)/groups/\(party.code)/links/\(linkId)", "fields": linkFields] as JSON,
            "currentDocument": ["exists": false] as JSON,
        ]
        let increment: JSON = ["fieldPath": "count", "increment": ["integerValue": "1"] as JSON]
        let countWrite: JSON = [
            "update": ["name": "\(base)/groups/\(party.code)/members/\(party.uid)"] as JSON,
            "updateMask": ["fieldPaths": [String]()] as JSON,
            "updateTransforms": [increment],
            "currentDocument": ["exists": true] as JSON,
        ]
        let body: JSON = ["writes": [linkWrite, countWrite]]
        let (_, code) = try await send(
            "https://firestore.googleapis.com/v1/\(base):commit", json: body, token: token
        )
        guard code == 200 else {
            throw ShareFailure(String(localized: "Couldn't add the reel. The party may have changed."))
        }
        return party.code
    }

    private func idToken(_ party: SharedParty) async throws -> String {
        guard let endpoint = URL(string: "https://securetoken.googleapis.com/v1/token?key=\(party.apiKey)") else {
            throw ShareFailure(String(localized: "Sharing isn't available in this build."))
        }
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._~"))
        let refresh = party.refreshToken.addingPercentEncoding(withAllowedCharacters: allowed) ?? ""
        request.httpBody = Data("grant_type=refresh_token&refresh_token=\(refresh)".utf8)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200,
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let token = object["id_token"] as? String else {
            throw ShareFailure(String(localized: "Your session expired. Open ScrollParty and try again."))
        }
        return token
    }

    private func partyStatus(_ party: SharedParty, base: String, token: String) async throws -> String {
        guard let endpoint = URL(string: "https://firestore.googleapis.com/v1/\(base)/groups/\(party.code)") else {
            throw ShareFailure(String(localized: "Open ScrollParty and join a party first."))
        }
        var request = URLRequest(url: endpoint)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await URLSession.shared.data(for: request)
        let code = (response as? HTTPURLResponse)?.statusCode ?? 0
        if code == 404 { throw ShareFailure(String(localized: "That party no longer exists.")) }
        guard code == 200,
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let fields = object["fields"] as? [String: Any],
              let status = (fields["status"] as? [String: Any])?["stringValue"] as? String else {
            throw ShareFailure(String(localized: "Couldn't reach the party. Check your connection and try again."))
        }
        return status
    }

    private func send(_ address: String, json: JSON, token: String) async throws -> (Data, Int) {
        guard let endpoint = URL(string: address) else { throw ShareFailure(String(localized: "Sharing isn't available in this build.")) }
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.httpBody = try JSONSerialization.data(withJSONObject: json)
        let (data, response) = try await URLSession.shared.data(for: request)
        return (data, (response as? HTTPURLResponse)?.statusCode ?? 0)
    }
}

typealias JSON = [String: Any]

struct ShareFailure: Error {
    let message: String
    init(_ message: String) { self.message = message }
}

/// Panel pequeño con el estilo de la Feria: trabajando, hecho (se cierra solo) o error con botón de cerrar.
struct SharePanel: View {
    @ObservedObject var model: ShareModel

    var body: some View {
        ZStack {
            Color.black.opacity(0.45).ignoresSafeArea()
            VStack(spacing: Space.s2) {
                switch model.state {
                case .working:
                    ProgressView().tint(Palette.yellow).controlSize(.large)
                    Text("Adding to the machine…").font(.spBody).foregroundStyle(Palette.white)
                case .done(let code):
                    DisplayTitle("In the machine", kind: .title)
                    Image(systemName: "checkmark.circle.fill").font(.largeTitle).foregroundStyle(Palette.mint)
                    Text("Party \(code)").font(.spBody).foregroundStyle(Palette.white)
                case .failed(let message):
                    DisplayTitle("Not added", kind: .title)
                    Text(verbatim: message)
                        .font(.spBody)
                        .foregroundStyle(Palette.white)
                        .multilineTextAlignment(.center)
                    PrimaryButton("Close") { model.close() }
                        .padding(.top, Space.s1)
                }
            }
            .padding(Space.s3)
            .frame(maxWidth: 340)
            .background(RoundedRectangle(cornerRadius: Space.r3).fill(Palette.blue))
            .padding(Space.margin)
            .accessibilityElement(children: .combine)
        }
        .task { await model.run() }
    }
}

final class ShareViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        let model = ShareModel(context: extensionContext)
        let host = UIHostingController(rootView: SharePanel(model: model).motionScope().preferredColorScheme(.dark))
        host.view.backgroundColor = .clear
        addChild(host)
        host.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(host.view)
        NSLayoutConstraint.activate([
            host.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            host.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            host.view.topAnchor.constraint(equalTo: view.topAnchor),
            host.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        host.didMove(toParent: self)
    }
}
