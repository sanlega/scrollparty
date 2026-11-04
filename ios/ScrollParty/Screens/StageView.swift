import SwiftUI

/// Hueco del vídeo: no se descargan vídeos, solo se abre el enlace en su app.
private struct VideoWindow: View {
    let url: String
    @Environment(\.openURL) private var openURL

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: 0xFF6A5F), Color(hex: 0xA331E0), Color(hex: 0x20124A)],
                startPoint: .top, endPoint: .bottom
            )
            Button {
                if let target = URL(string: url) { openURL(target) }
            } label: {
                Image(systemName: "play.fill")
                    .font(.system(size: 36))
                    .foregroundStyle(Palette.white)
                    .frame(width: 96, height: 96)
                    .background(Circle().fill(Palette.night.opacity(0.55)))
            }
            .accessibilityLabel(Text("Play reel"))
        }
        .aspectRatio(306.0 / 372.0, contentMode: .fit)
    }
}

/// Votación del anfitrión: rejilla neutra a pantalla completa. No marca lo elegido, ni al reabrirla.
private struct SecretVotePanel: View {
    let candidates: [Member]
    let onVote: (Member) -> Void
    let onClose: () -> Void

    var body: some View {
        ZStack {
            Palette.night.ignoresSafeArea()
            ScrollView {
                VStack(spacing: Space.s2) {
                    DisplayTitle("Secret vote", kind: .title)
                    Text("Nothing here shows who you picked.")
                        .font(.spBody)
                        .foregroundStyle(Palette.white)
                        .multilineTextAlignment(.center)
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 96), spacing: 10)], spacing: 10) {
                        ForEach(candidates) { member in
                            Button {
                                Haptics.impact(.medium)
                                onVote(member)
                            } label: {
                                VStack(spacing: 4) {
                                    PlayerFace(name: member.name, color: member.color, photo: member.photo)
                                    Text(verbatim: member.name)
                                        .font(.spCaption)
                                        .foregroundStyle(Palette.white)
                                        .lineLimit(1)
                                }
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(Text(verbatim: member.name))
                        }
                    }
                    .padding(.top, Space.s1)
                    LinkButton(title: "Back", action: onClose)
                }
                .padding(.horizontal, Space.margin)
                .padding(.vertical, Space.s3)
            }
        }
    }
}

/// La tele (anfitrión): vídeo grande, cuántos han votado, voto secreto y reacciones volando.
struct StageView: View {
    let model: GameModel
    let group: Party
    let round: Round
    var openSecret = false
    @Environment(\.motion) private var motion
    @State private var showSecret = false

    private var expected: Int { max(0, model.members.count - 1) }
    private var mine: Bool { model.myLinks.contains { $0.id == round.id } }
    private var myVote: String? { model.uid.flatMap { round.votes[$0] } }
    private var candidates: [Member] { model.members.filter { $0.id != model.uid } }

    var body: some View {
        ZStack {
            ScreenFrame(night: true) {
                Text("Reel \(group.roundIndex + 1) of \(group.roundIds.count)")
                    .spLabelStyle(Palette.yellow)
                    .frame(maxWidth: .infinity)
                    .padding(.top, Space.s1)
                Machine(lights: true) { VideoWindow(url: round.url) }
                    .frame(maxWidth: 306)
                    .frame(maxWidth: .infinity)
                    .padding(.top, Space.s1)
                DisplayTitle("\(round.votes.count) of \(expected) voted", kind: .title, color: Palette.white)
                    .padding(.top, Space.s3)
                Spacer(minLength: Space.s3)
                voteArea
                PrimaryButton("Reveal who sent it") { model.reveal(round) }
                    .padding(.top, Space.s2)
            }
            FlyingReactions(reactions: round.reactions)
                .padding(.horizontal, Space.margin)
            if showSecret {
                SecretVotePanel(candidates: candidates, onVote: { member in
                    model.vote(round, guess: member.id)
                    showSecret = false
                }, onClose: { showSecret = false })
                .transition(motion.sheetTransition)
            }
        }
        .animation(motion.snap, value: showSecret)
        .onAppear { if openSecret { showSecret = true } }
        // Revela solo cuando han votado todos; el botón es para forzarlo si alguien tarda.
        .task(id: "\(round.id)-\(round.votes.count)") {
            if expected > 0, round.votes.count >= expected { model.reveal(round) }
        }
    }

    /// El mismo mensaje sea quien sea el elegido: la tele nunca delata el voto.
    @ViewBuilder private var voteArea: some View {
        if mine {
            Text("This one’s yours")
                .font(.spBody)
                .foregroundStyle(Palette.soft)
                .frame(maxWidth: .infinity, minHeight: 56)
        } else {
            Button { showSecret = true } label: {
                Group {
                    if myVote != nil {
                        Label("Your vote is in · Change", systemImage: "checkmark")
                    } else {
                        Text("Cast your secret vote")
                    }
                }
                .font(.spBody)
                .foregroundStyle(Palette.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: 56)
                .overlay(RoundedRectangle(cornerRadius: Space.r1).strokeBorder(Palette.soft, lineWidth: 2))
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }
}
