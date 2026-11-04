import SwiftUI

/// Premio del final: la cara de quien lo gana (o de todos los empatados, con sus nombres).
struct PrizeCard: View {
    let title: LocalizedStringKey
    let winners: [Member]

    var body: some View {
        VStack(spacing: 6) {
            if let first = winners.first {
                PlayerFace(name: first.name, color: first.color, photo: first.photo, size: 46)
            }
            Text(title).font(.spLabel).foregroundStyle(Palette.ink).multilineTextAlignment(.center)
            Text(verbatim: winners.map(\.name).joined(separator: ", "))
                .font(.spCaption)
                .foregroundStyle(Palette.inkSoft)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 4)
        .padding(.vertical, Space.s1)
        .background(RoundedRectangle(cornerRadius: Space.r1).fill(Palette.white))
        .accessibilityElement(children: .combine)
    }
}
