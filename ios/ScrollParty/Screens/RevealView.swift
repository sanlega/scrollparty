import SwiftUI

/// Revelado: rodillo con las caras, resultado, tique y quién votó qué.
struct RevealView: View {
    let model: GameModel
    let group: Party
    let round: Round
    @State private var done = false

    private var author: Member? { model.members.first { $0.id == round.authorId } }
    private var myVote: String? { model.uid.flatMap { round.votes[$0] } }
    private var iAmAuthor: Bool { round.authorId != nil && round.authorId == model.uid }
    private var voters: [Member] {
        model.members.filter { round.votes[$0.id] != nil && $0.id != round.authorId }
    }
    private var isLastRound: Bool { group.roundIndex + 1 >= group.roundIds.count }

    var body: some View {
        ScreenFrame {
            if let author {
                ReelReveal(author: author, others: model.members) { done = true }
                    .frame(maxWidth: 290)
                    .frame(maxWidth: .infinity)
                    .padding(.top, Space.s1)
            }
            VStack(spacing: Space.s1) {
                DisplayTitle(verbatim: (author?.name ?? "?") + "!")
                    .padding(.top, Space.s2)
                ticket
                ForEach(voters) { voter in voterRow(voter) }
            }
            .opacity(done ? 1 : 0)
            .accessibilityHidden(!done)
            .animation(.easeInOut(duration: 0.15), value: done)
            Spacer(minLength: Space.s2)
            if model.isAdmin {
                PrimaryButton(isLastRound ? "See the final tickets" : "Next reel") { model.next() }
                    .padding(.top, Space.s2)
            } else {
                Text("Waiting for the host…")
                    .font(.spBody)
                    .foregroundStyle(Palette.soft)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, Space.s2)
            }
        }
        .confetti(done)
        .onAppear { if author == nil { done = true } }
    }

    @ViewBuilder private var ticket: some View {
        if let authorId = round.authorId {
            if iAmAuthor {
                let fooled = round.votes.filter { $0.key != authorId && $0.value != authorId }.count
                if fooled == 1 {
                    Ticket(Text("+1 ticket"))
                } else if fooled > 1 {
                    Ticket(Text("+\(fooled) tickets"))
                }
            } else if myVote == authorId {
                Ticket(Text("You got it · +1 ticket"))
            } else if myVote != nil {
                Ticket(Text("Fooled"), fooled: true)
            }
        }
    }

    private func voterRow(_ voter: Member) -> some View {
        let guess = model.members.first { $0.id == round.votes[voter.id] }
        let right = round.votes[voter.id] == round.authorId
        return HStack(spacing: 11) {
            PlayerFace(name: voter.name, color: voter.color, photo: voter.photo, size: 42, radius: 10)
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: voter.id == model.uid ? String(localized: "You") : voter.name)
                    .font(.spBody)
                    .foregroundStyle(Palette.white)
                    .lineLimit(1)
                Text("guessed \(guess?.name ?? "?")")
                    .font(.spCaption)
                    .foregroundStyle(Palette.soft)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            Ticket(right ? Text("Right") : Text("Fooled"), fooled: !right, small: true)
        }
        .frame(minHeight: 54)
        .accessibilityElement(children: .combine)
    }
}
