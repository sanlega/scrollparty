import SwiftUI

private let emojis = ["😎", "🤠", "👻", "🦄", "🐸", "🍕"]
private let pink = Color(red: 1, green: 0.24, blue: 0.51)

@main
struct ScrollPartyApp: App {
    var body: some Scene {
        WindowGroup {
            // Los tests lógicos no deben arrancar Firebase en la app anfitriona.
            if NSClassFromString("XCTestCase") == nil {
                RootView()
                    .preferredColorScheme(.dark)
                    .tint(pink)
            }
        }
    }
}

struct RootView: View {
    @State private var model = GameModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if let error = model.error {
                    Text(error).foregroundStyle(.red)
                }
                if model.uid == nil {
                    ProgressView()
                } else if let group = model.group {
                    switch group.status {
                    case "collecting": LobbyView(model: model, group: group)
                    case "playing": SessionView(model: model, group: group)
                    default: ResultsView(model: model)
                    }
                } else {
                    HomeView(model: model)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color(red: 0.07, green: 0.05, blue: 0.12))
    }
}

private struct PartyTitle: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View { Text(text).font(.largeTitle.bold()) }
}

private struct WideButton: View {
    let title: String
    var prominent = true
    let action: () -> Void

    var body: some View {
        if prominent {
            Button(action: action) { Text(title).frame(maxWidth: .infinity) }
                .buttonStyle(.borderedProminent).controlSize(.large)
        } else {
            Button(action: action) { Text(title).frame(maxWidth: .infinity) }
                .buttonStyle(.bordered).controlSize(.large)
        }
    }
}

struct HomeView: View {
    let model: GameModel
    @State private var name = ""
    @State private var emoji = "😎"
    @State private var code = ""

    private var trimmedName: String { name.trimmingCharacters(in: .whitespaces) }

    var body: some View {
        PartyTitle("ScrollParty")
        Text("Drop your best reels in the bag. Guess who sent what.")
        TextField("Your name", text: $name).textFieldStyle(.roundedBorder)
        HStack {
            ForEach(emojis, id: \.self) { item in
                Button { emoji = item } label: {
                    Text(item).font(item == emoji ? .largeTitle : .title3)
                }
                .frame(maxWidth: .infinity)
            }
        }
        WideButton(title: "Create group") { model.create(name: trimmedName, emoji: emoji) }
            .disabled(trimmedName.isEmpty)
        Spacer().frame(height: 12)
        TextField("Group code", text: $code)
            .textFieldStyle(.roundedBorder)
            .textInputAutocapitalization(.characters)
            .autocorrectionDisabled()
        WideButton(title: "Join group", prominent: false) { model.joinGroup(code: code, name: trimmedName, emoji: emoji) }
            .disabled(trimmedName.isEmpty || code.count != 6)
            .onAppear {
                name = model.name
                emoji = model.emoji
            }
    }
}

struct LobbyView: View {
    let model: GameModel
    let group: Group
    @State private var link = ""

    var body: some View {
        Text("Group code").foregroundStyle(.secondary)
        PartyTitle(group.code).textSelection(.enabled)
        VStack(alignment: .leading, spacing: 6) {
            ForEach(model.members) { member in
                Text("\(member.emoji)  \(member.name)\(member.id == group.adminId ? " (host)" : "")  ·  \(member.count) reels")
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))

        TextField("Paste a reel, TikTok or Short", text: $link)
            .textFieldStyle(.roundedBorder)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .keyboardType(.URL)
        WideButton(title: "Add to the bag") {
            model.addLink(link)
            link = ""
        }
        .disabled(link.trimmingCharacters(in: .whitespaces).isEmpty)

        if !model.myLinks.isEmpty {
            Text("Your reels").foregroundStyle(.secondary)
        }
        ForEach(model.myLinks) { item in
            HStack {
                Text(item.url).lineLimit(1)
                Spacer()
                Button("Remove") { model.removeLink(item) }
            }
        }

        if model.isAdmin {
            WideButton(title: "Start the party") { model.start() }
                .disabled(model.members.count < 2 || !model.members.contains { $0.count > 0 })
        } else {
            Text("Waiting for the host to start…").foregroundStyle(.secondary)
        }
        Button("Leave") { model.forget() }
    }
}

struct SessionView: View {
    let model: GameModel
    let group: Group
    @Environment(\.openURL) private var openURL

    var body: some View {
        if let round = model.currentRound {
            content(round)
        } else {
            Text("Getting the bag ready…")
            if model.isAdmin {
                Button("Retry") { model.start() }
            }
        }
    }

    @ViewBuilder
    private func content(_ round: Round) -> some View {
        let expected = model.members.count - 1
        let author = model.members.first { $0.id == round.authorId }
        let revealed = round.authorId != nil
        let mine = model.myLinks.contains { $0.id == round.id }
        let myVote = model.uid.flatMap { round.votes[$0] }

        Text("Reel \(group.roundIndex + 1) of \(group.roundIds.count)").foregroundStyle(.secondary)
        PartyTitle(revealed ? "It was \(author?.emoji ?? "") \(author?.name ?? "someone")!" : "Who sent this?")

        if model.isAdmin {
            WideButton(title: "▶  Play reel") {
                if let url = URL(string: round.url) { openURL(url) }
            }
        }

        if !revealed {
            if mine {
                Text("This one's yours 🤫 Keep a straight face.").font(.title3)
            } else {
                ForEach(model.members.filter { $0.id != model.uid }) { member in
                    WideButton(title: "\(member.emoji)  \(member.name)", prominent: myVote == member.id) {
                        model.vote(round, guess: member.id)
                    }
                }
            }
            Text("\(round.votes.count)/\(expected) have voted")
                .font(.title2)
                .frame(maxWidth: .infinity)
        } else {
            if !mine {
                Text(myVote == round.authorId ? "You got it! +1" : "Fooled you!").font(.title3)
            }
            ForEach(model.members.filter { round.votes[$0.id] != nil }) { member in
                let guess = model.members.first { $0.id == round.votes[member.id] }
                Text("\(member.emoji) \(member.name) guessed \(guess?.name ?? "?") \(guess?.id == round.authorId ? "✅" : "❌")")
            }
        }

        if model.isAdmin {
            Group2 {
                if revealed {
                    WideButton(title: "Next") { model.next() }
                } else {
                    Button("Reveal now") { model.reveal(round) }
                        .frame(maxWidth: .infinity)
                }
            }
            // Revela solo cuando han votado todos; el botón es para forzarlo si alguien tarda.
            .task(id: "\(round.id)-\(round.votes.count)-\(revealed)") {
                if !revealed, expected > 0, round.votes.count >= expected { model.reveal(round) }
            }
        }
    }
}

/// `Group` de SwiftUI queda tapado por el modelo `Group` del juego.
private typealias Group2 = SwiftUI.Group

struct ResultsView: View {
    let model: GameModel

    private func names(_ ids: [String]) -> String {
        ids.compactMap { id in model.members.first { $0.id == id }.map { "\($0.emoji) \($0.name)" } }
            .joined(separator: ", ")
    }

    var body: some View {
        let table = standings(rounds: model.rounds, memberIds: model.members.map(\.id))
        let detectives = winners(table) { $0.correct }
        let infiltrators = winners(table) { $0.fooled }

        PartyTitle("Final scores")
        if !detectives.isEmpty { Text("🕵️ Detective: \(names(detectives))").font(.title3) }
        if !infiltrators.isEmpty { Text("🥷 Infiltrator: \(names(infiltrators))").font(.title3) }
        VStack(alignment: .leading, spacing: 6) {
            ForEach(Array(table.enumerated()), id: \.element.memberId) { index, row in
                let member = model.members.first { $0.id == row.memberId }
                Text("\(index + 1). \(member?.emoji ?? "") \(member?.name ?? "") — \(row.score)").font(.title3)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
        Button("Leave") { model.forget() }
    }
}
