import SwiftUI

/// Único punto donde se lee «reducir movimiento». Con él: fundidos de 150 ms y nada de rodillo, confeti ni rayos.
struct Motion {
    var reduced = false

    private var fade: Animation { .easeInOut(duration: 0.15) }

    var spring: Animation { reduced ? fade : .timingCurve(0.3, 1.7, 0.5, 1, duration: 0.4) }
    var snap: Animation { reduced ? fade : .timingCurve(0.2, 0.8, 0.2, 1, duration: 0.32) }
    var press: Animation? { reduced ? nil : .easeOut(duration: 0.08) }
    var reel: Animation { .timingCurve(0.15, 0.7, 0.2, 1.04, duration: 1.5) }
    var sheetTransition: AnyTransition { reduced ? .opacity : .move(edge: .bottom) }
}

private struct MotionKey: EnvironmentKey {
    static let defaultValue = Motion()
}

extension EnvironmentValues {
    var motion: Motion {
        get { self[MotionKey.self] }
        set { self[MotionKey.self] = newValue }
    }
}

private struct MotionScope: ViewModifier {
    @Environment(\.accessibilityReduceMotion) private var reduce

    func body(content: Content) -> some View {
        content.environment(\.motion, Motion(reduced: reduce))
    }
}

private struct Pop: ViewModifier {
    @Environment(\.motion) private var motion
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(shown || motion.reduced ? 1 : 0.3)
            .rotationEffect(.degrees(shown || motion.reduced ? 0 : -8))
            .opacity(shown ? 1 : 0)
            .onAppear { withAnimation(motion.spring) { shown = true } }
    }
}

extension View {
    /// Se pone una vez en la raíz de la app (y de la extensión).
    func motionScope() -> some View { modifier(MotionScope()) }
    /// Aparición con rebote; con movimiento reducido, un fundido.
    func pop() -> some View { modifier(Pop()) }
}
