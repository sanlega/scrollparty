import SwiftUI

/// Hilera de luces que parpadean (quietas con movimiento reducido).
struct MachineLights: View {
    @Environment(\.motion) private var motion
    @State private var phase = false

    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<13, id: \.self) { index in
                if index > 0 { Spacer(minLength: 0) }
                let even = index % 2 == 0
                Circle()
                    .fill(even ? Palette.coral : Palette.blue)
                    .frame(width: 10, height: 10)
                    .opacity(motion.reduced ? 1 : ((phase == even) ? 1 : 0.35))
            }
        }
        .padding(.horizontal, 8)
        .padding(.bottom, 9)
        .accessibilityHidden(true)
        .onAppear {
            guard !motion.reduced else { return }
            withAnimation(.linear(duration: 0.9).repeatForever(autoreverses: true)) { phase = true }
        }
    }
}

/// Carcasa amarilla con labio y ventana blanca.
struct Machine<Content: View>: View {
    let lights: Bool
    let content: Content

    init(lights: Bool = false, @ViewBuilder content: () -> Content) {
        self.lights = lights
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            if lights { MachineLights() }
            content
                .frame(maxWidth: .infinity)
                .background(Palette.white)
                .overlay(alignment: .top) {
                    LinearGradient(colors: [Color.black.opacity(0.2), .clear], startPoint: .top, endPoint: .bottom)
                        .frame(height: 10)
                        .allowsHitTesting(false)
                }
                .clipShape(RoundedRectangle(cornerRadius: Space.r2))
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: Space.r3)
                .fill(Palette.yellow)
                .shadow(color: Palette.yellowDeep, radius: 0, x: 0, y: 8)
        )
        .padding(.bottom, 8)
    }
}
