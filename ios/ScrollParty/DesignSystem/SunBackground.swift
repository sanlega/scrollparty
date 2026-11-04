import SwiftUI

/// Rayos vectoriales del fondo. Giran despacio; con movimiento reducido, quietos.
struct SunBackground: View {
    var night = false
    @Environment(\.motion) private var motion

    var body: some View {
        ZStack {
            (night ? Palette.night : Palette.blue)
            if !night {
                if motion.reduced {
                    rays(angle: 0)
                } else {
                    TimelineView(.animation(minimumInterval: 1.0 / 20.0)) { context in
                        rays(angle: context.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 60) * 6)
                    }
                }
            }
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }

    private func rays(angle: Double) -> some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height * 0.38)
            let radius = hypot(size.width, size.height)
            for index in 0..<15 {
                let start = angle + Double(index) * 24
                var path = Path()
                path.move(to: center)
                path.addArc(center: center, radius: radius, startAngle: .degrees(start), endAngle: .degrees(start + 12), clockwise: false)
                path.closeSubpath()
                context.fill(path, with: .color(Palette.blueDeep))
            }
        }
    }
}

/// Fondo + contenido que se estira hasta ocupar la pantalla y hace scroll si no cabe (Dynamic Type grande).
struct ScreenFrame<Content: View>: View {
    let night: Bool
    let content: Content

    init(night: Bool = false, @ViewBuilder content: () -> Content) {
        self.night = night
        self.content = content()
    }

    var body: some View {
        ZStack {
            SunBackground(night: night)
            GeometryReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) { content }
                        .padding(.horizontal, Space.margin)
                        .padding(.vertical, Space.s2)
                        .frame(maxWidth: .infinity, minHeight: proxy.size.height)
                }
                .scrollBounceBehavior(.basedOnSize)
            }
        }
    }
}
