import SwiftUI

struct StepBar: View {
    let step: Int
    let total: Int

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<total, id: \.self) { index in
                Capsule()
                    .fill(index <= step ? Palette.yellow : Palette.white.opacity(0.33))
                    .frame(height: 7)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Step \(step + 1) of \(total)"))
    }
}
