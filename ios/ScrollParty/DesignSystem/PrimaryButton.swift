import SwiftUI

enum LipStyle {
    case primary, alt
}

/// Aspecto del botón con labio de 7 pt. Se usa suelto en etiquetas de PhotosPicker y ShareLink.
struct LipLabel: View {
    let title: LocalizedStringKey
    var style: LipStyle = .primary
    var loading = false
    var pressed = false
    var enabled = true

    private var face: Color {
        if !enabled { return Palette.night2 }
        return style == .primary ? Palette.yellow : Palette.white
    }

    private var lip: Color {
        if !enabled { return .clear }
        return style == .primary ? Palette.yellowDeep : Palette.lipAlt
    }

    var body: some View {
        Group {
            if loading {
                ProgressView().tint(Palette.ink)
            } else {
                Text(title)
                    .font(.spButton)
                    .textCase(.uppercase)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.6)
                    .foregroundStyle(enabled ? Palette.ink : Palette.soft)
            }
        }
        .padding(.horizontal, Space.s2)
        .padding(.vertical, Space.s1)
        .frame(maxWidth: .infinity, minHeight: 64)
        .background(
            RoundedRectangle(cornerRadius: Space.r2)
                .fill(face)
                .overlay(
                    RoundedRectangle(cornerRadius: Space.r2)
                        .strokeBorder(Palette.white.opacity(enabled ? 0 : 0.33), lineWidth: 2)
                )
                .shadow(color: lip, radius: 0, x: 0, y: pressed ? 1 : 7)
        )
        .offset(y: pressed && enabled ? 6 : 0)
        .padding(.bottom, 7)
        .contentShape(Rectangle())
    }
}

private struct LipButtonStyle: ButtonStyle {
    let title: LocalizedStringKey
    let style: LipStyle
    let loading: Bool

    func makeBody(configuration: Configuration) -> some View {
        LipBody(title: title, style: style, loading: loading, pressed: configuration.isPressed)
    }
}

private struct LipBody: View {
    let title: LocalizedStringKey
    let style: LipStyle
    let loading: Bool
    let pressed: Bool
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.motion) private var motion

    var body: some View {
        LipLabel(title: title, style: style, loading: loading, pressed: pressed, enabled: isEnabled)
            .animation(motion.press, value: pressed)
    }
}

/// Botón primario (amarillo) o `alt` (blanco). Estados: normal, pulsado, desactivado y cargando.
struct PrimaryButton: View {
    let title: LocalizedStringKey
    var style: LipStyle = .primary
    var loading = false
    let action: () -> Void

    init(_ title: LocalizedStringKey, style: LipStyle = .primary, loading: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.style = style
        self.loading = loading
        self.action = action
    }

    var body: some View {
        Button {
            if !loading { action() }
        } label: {
            Text(title)
        }
        .buttonStyle(LipButtonStyle(title: title, style: style, loading: loading))
    }
}
