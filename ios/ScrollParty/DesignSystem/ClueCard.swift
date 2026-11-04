import SwiftUI

/// Pista anónima que dejó el autor al añadir el reel.
struct ClueCard: View {
    let clue: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Anonymous clue").spLabelStyle(Palette.inkSoft)
            Text(verbatim: "“" + clue + "”").font(.spBody).foregroundStyle(Palette.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Space.s2)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: Space.r1).fill(Palette.white))
        .accessibilityElement(children: .combine)
    }
}
