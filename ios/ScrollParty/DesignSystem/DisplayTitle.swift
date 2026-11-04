import SwiftUI

enum DisplayKind {
    case display, title, heading
}

/// Titular Bowlby One en mayúsculas: se ajusta solo, sin saltos manuales.
struct DisplayTitle: View {
    let text: Text
    var kind: DisplayKind = .display
    var color: Color = Palette.yellow
    var alignment: TextAlignment = .center

    init(_ key: LocalizedStringKey, kind: DisplayKind = .display, color: Color = Palette.yellow, alignment: TextAlignment = .center) {
        self.text = Text(key)
        self.kind = kind
        self.color = color
        self.alignment = alignment
    }

    /// Para nombres y otros textos que no se traducen.
    init(verbatim: String, kind: DisplayKind = .display, color: Color = Palette.yellow, alignment: TextAlignment = .center) {
        self.text = Text(verbatim: verbatim)
        self.kind = kind
        self.color = color
        self.alignment = alignment
    }

    private var font: Font {
        switch kind {
        case .display: return .spDisplay
        case .title: return .spTitle
        case .heading: return .spHeading
        }
    }

    var body: some View {
        text
            .font(font)
            .textCase(.uppercase)
            .foregroundStyle(color)
            .shadow(color: Palette.blueDeep, radius: 0, x: 0, y: kind == .heading ? 0 : 4)
            .multilineTextAlignment(alignment)
            .lineLimit(3)
            .minimumScaleFactor(0.6)
            .frame(maxWidth: .infinity, alignment: alignment == .center ? .center : .leading)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityAddTraits(.isHeader)
    }
}
