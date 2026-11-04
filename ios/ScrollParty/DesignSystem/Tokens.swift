import SwiftUI

/// Valores del design system «La Feria» (docs/design/sistema.md). Única fuente: los tests miden el contraste con ellos.
enum Hex {
    static let blue: UInt32 = 0x2323FF
    static let blueDeep: UInt32 = 0x1616C9
    static let night: UInt32 = 0x0B0B3B
    static let night2: UInt32 = 0x17175C
    static let yellow: UInt32 = 0xFFE600
    static let yellowDeep: UInt32 = 0xB3A000
    static let coral: UInt32 = 0xFF4D2E
    static let mint: UInt32 = 0x16E0A6
    static let blush: UInt32 = 0xFFC9BF
    static let white: UInt32 = 0xFFFFFF
    static let ink: UInt32 = 0x14143C
    static let inkSoft: UInt32 = 0x4A4A75
    static let soft: UInt32 = 0xD9D9FF
    static let player: [UInt32] = [
        0xFF4D2E, 0x16E0A6, 0xFF4FD8, 0xFFB400, 0x2FB6FF, 0xFFE600,
        0xB06BFF, 0x7BEA3A, 0xFF7A59, 0x3DE0E0, 0xFF5C9E, 0xFFC14D,
    ]

    /// Pares texto/fondo que usa la interfaz (mínimo 4,5:1). Los comprueba un test.
    static let textPairs: [(name: String, text: UInt32, background: UInt32)] = [
        ("ink/yellow", ink, yellow), ("ink/white", ink, white), ("ink/mint", ink, mint), ("ink/blush", ink, blush),
        ("yellow/blue", yellow, blue), ("yellow/night", yellow, night),
        ("white/blue", white, blue), ("white/night", white, night),
        ("soft/blue", soft, blue), ("soft/night", soft, night), ("soft/night2", soft, night2),
        ("inkSoft/white", inkSoft, white), ("blue/white", blue, white),
    ]
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

/// `Color.blue` y compañía son de SwiftUI: los colores de la Feria viven aquí.
enum Palette {
    static let blue = Color(hex: Hex.blue)
    static let blueDeep = Color(hex: Hex.blueDeep)
    static let night = Color(hex: Hex.night)
    static let night2 = Color(hex: Hex.night2)
    static let yellow = Color(hex: Hex.yellow)
    static let yellowDeep = Color(hex: Hex.yellowDeep)
    static let coral = Color(hex: Hex.coral)
    static let mint = Color(hex: Hex.mint)
    static let blush = Color(hex: Hex.blush)
    static let white = Color(hex: Hex.white)
    static let ink = Color(hex: Hex.ink)
    static let inkSoft = Color(hex: Hex.inkSoft)
    static let soft = Color(hex: Hex.soft)
    /// Sombra del labio del botón blanco.
    static let lipAlt = Color(hex: 0x9C9CC9)

    static func player(_ index: Int) -> Color {
        Color(hex: Hex.player[((index % Hex.player.count) + Hex.player.count) % Hex.player.count])
    }
}

/// Contraste WCAG entre dos colores 0xRRGGBB.
func contrastRatio(_ a: UInt32, _ b: UInt32) -> Double {
    func luminance(_ hex: UInt32) -> Double {
        func channel(_ shift: UInt32) -> Double {
            let c = Double((hex >> shift) & 0xFF) / 255
            return c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
    let la = luminance(a)
    let lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

enum Space {
    static let s1: CGFloat = 8
    static let s2: CGFloat = 16
    static let s3: CGFloat = 24
    static let s4: CGFloat = 32
    static let margin: CGFloat = 24
    static let r1: CGFloat = 14
    static let r2: CGFloat = 22
    static let r3: CGFloat = 30
    static let touch: CGFloat = 48
}

/// Todas escalan con el tamaño de texto del sistema (`relativeTo:`).
extension Font {
    private static let bowlby = "BowlbyOne-Regular"
    private static let dmMedium = "DMSans-14ptMedium"
    private static let dmBold = "DMSans-14ptBold"

    static let spDisplay = Font.custom(bowlby, size: 46, relativeTo: .largeTitle)
    static let spTitle = Font.custom(bowlby, size: 34, relativeTo: .title)
    static let spButton = Font.custom(bowlby, size: 21, relativeTo: .headline)
    static let spHeading = Font.custom(bowlby, size: 22, relativeTo: .title3)
    static let spBody = Font.custom(dmBold, size: 19, relativeTo: .body)
    static let spLabel = Font.custom(dmBold, size: 14, relativeTo: .footnote)
    static let spCaption = Font.custom(dmMedium, size: 14, relativeTo: .footnote)
    static let spTicket = Font.custom(bowlby, size: 18, relativeTo: .headline)
    /// Cifras grandes de la máquina.
    static func spNumber(_ size: CGFloat) -> Font { Font.custom(bowlby, size: size, relativeTo: .largeTitle) }
}

extension View {
    /// Etiqueta en mayúsculas con tracking 0.06em.
    func spLabelStyle(_ color: Color = Palette.soft) -> some View {
        self.font(.spLabel).textCase(.uppercase).tracking(0.84).foregroundStyle(color)
    }
}
