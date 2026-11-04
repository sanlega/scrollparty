import SwiftUI

/// El autor del reel: ve su cara, no vota y reacciona con los demás.
struct YoursView: View {
    let model: GameModel
    let round: Round

    private var me: Member? { model.members.first { $0.id == model.uid } }
    private var myReaction: Reaction? { model.uid.flatMap { round.reactions[$0] }.flatMap(Reaction.init(rawValue:)) }

    var body: some View {
        ScreenFrame {
            Spacer(minLength: Space.s3)
            Machine {
                PlayerFace(name: me?.name ?? model.name, color: me?.color ?? model.color, photo: me?.photo ?? model.photo, radius: 0)
                    .frame(maxWidth: .infinity)
            }
            .frame(maxWidth: 230)
            .frame(maxWidth: .infinity)
            .pop()
            DisplayTitle("This one’s yours")
                .padding(.top, Space.s4)
            Text("No vote this round. Keep a straight face and act surprised.")
                .font(.spBody)
                .foregroundStyle(Palette.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Space.s2)
            Spacer(minLength: Space.s3)
            ReactionBar(selected: myReaction) { model.react(round, $0) }
        }
    }
}
