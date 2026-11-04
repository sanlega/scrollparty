import SwiftUI

/// Votantes: pista, rueda de sospechosos, reacciones y «That’s my guess». Se puede cambiar hasta el revelado.
struct PickView: View {
    let model: GameModel
    let group: Party
    let round: Round
    @State private var centered: String?

    private var candidates: [Member] { model.members.filter { $0.id != model.uid } }
    private var myVote: String? { model.uid.flatMap { round.votes[$0] } }
    private var myReaction: Reaction? { model.uid.flatMap { round.reactions[$0] }.flatMap(Reaction.init(rawValue:)) }
    private var lockedMember: Member? {
        guard let vote = myVote, vote == centered else { return nil }
        return candidates.first { $0.id == vote }
    }

    var body: some View {
        ScreenFrame {
            Text("Reel \(group.roundIndex + 1) of \(group.roundIds.count) · watch the TV")
                .spLabelStyle(Palette.yellow)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Space.s1)
            if !round.clue.isEmpty {
                ClueCard(clue: round.clue).padding(.top, Space.s1)
            }
            DisplayTitle("Who sent it?", alignment: .leading)
                .padding(.vertical, Space.s2)
            SuspectWheel(members: candidates, centered: $centered)
            Spacer(minLength: Space.s2)
            ReactionBar(selected: myReaction) { model.react(round, $0) }
            if let locked = lockedMember {
                Text("\(locked.name) locked in · scroll to change")
                    .font(.spButton)
                    .textCase(.uppercase)
                    .foregroundStyle(Palette.ink)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.6)
                    .frame(maxWidth: .infinity, minHeight: 64)
                    .padding(.horizontal, Space.s2)
                    .background(RoundedRectangle(cornerRadius: Space.r2).fill(Palette.mint))
                    .padding(.top, Space.s2)
                    .accessibilityAddTraits(.isStaticText)
            } else {
                PrimaryButton("That’s my guess") {
                    if let id = centered {
                        Haptics.impact(.medium)
                        model.vote(round, guess: id)
                    }
                }
                .disabled(centered == nil)
                .padding(.top, Space.s2)
            }
        }
        .onAppear {
            if centered == nil { centered = myVote ?? candidates.first?.id }
        }
    }
}
