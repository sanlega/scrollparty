import SwiftUI

/// Campo de texto blanco con etiqueta; el estilo de teclado lo pone quien lo usa.
struct SPField: View {
    let title: LocalizedStringKey
    let prompt: LocalizedStringKey
    @Binding var text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).spLabelStyle(Palette.white)
            TextField("", text: $text, prompt: Text(prompt).foregroundStyle(Palette.inkSoft))
                .font(.spBody)
                .foregroundStyle(Palette.ink)
                .padding(.horizontal, Space.s2)
                .frame(minHeight: 58)
                .background(RoundedRectangle(cornerRadius: Space.r1).fill(Palette.white))
                .accessibilityLabel(Text(title))
        }
    }
}

/// Enlace de texto con zona táctil de 48 pt.
struct LinkButton: View {
    let title: LocalizedStringKey
    let action: () -> Void
    @Environment(\.isEnabled) private var isEnabled

    init(_ title: LocalizedStringKey, action: @escaping () -> Void) {
        self.title = title
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.spBody)
                .foregroundStyle(isEnabled ? Palette.white : Palette.soft)
                .underline(!isEnabled)
                .frame(maxWidth: .infinity, minHeight: Space.touch)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
