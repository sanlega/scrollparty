import SwiftUI

/// Final: tiques de cada jugador y premios.
struct FinalView: View {
    let model: GameModel

    private func members(_ ids: [String]) -> [Member] {
        ids.compactMap { id in model.members.first { $0.id == id } }
    }

    var body: some View {
        let table = standings(rounds: model.rounds, memberIds: model.members.map(\.id))
        let detectives = members(winners(table) { $0.correct })
        let bluffers = members(winners(table) { $0.fooled })
        let reel = members(reelOfTheNight(rounds: model.playedRounds).map { [$0] } ?? [])

        ScreenFrame {
            DisplayTitle("Final tickets")
                .padding(.top, Space.s2)
            VStack(spacing: 0) {
                ForEach(Array(table.enumerated()), id: \.element.memberId) { index, row in
                    if let member = model.members.first(where: { $0.id == row.memberId }) {
                        HStack(spacing: 10) {
                            Text(verbatim: "\(index + 1)").font(.spBody).foregroundStyle(Palette.ink).frame(minWidth: 18)
                            PlayerFace(name: member.name, color: member.color, photo: member.photo, size: 38, radius: 9)
                            Text(verbatim: member.id == model.uid ? String(localized: "You") : member.name)
                                .font(.spBody)
                                .foregroundStyle(Palette.ink)
                                .lineLimit(1)
                            Spacer(minLength: 0)
                            Text(verbatim: "\(row.score)").font(.spHeading).foregroundStyle(Palette.ink)
                        }
                        .frame(minHeight: 46)
                        .accessibilityElement(children: .combine)
                    }
                }
            }
            .padding(14)
            .background(RoundedRectangle(cornerRadius: 6).fill(Palette.white))
            .rotationEffect(.degrees(-1.4))
            .padding(.top, Space.s2)

            if !detectives.isEmpty || !bluffers.isEmpty || !reel.isEmpty {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 104), spacing: Space.s1, alignment: .top)], spacing: Space.s1) {
                    if !detectives.isEmpty { PrizeCard(title: "Top Detective", winners: detectives) }
                    if !bluffers.isEmpty { PrizeCard(title: "Master Bluffer", winners: bluffers) }
                    if !reel.isEmpty { PrizeCard(title: "Reel of the Night", winners: reel) }
                }
                .padding(.top, Space.s3)
            }
            Spacer(minLength: Space.s3)
            LinkButton("Leave party") { model.forget() }
        }
    }
}
