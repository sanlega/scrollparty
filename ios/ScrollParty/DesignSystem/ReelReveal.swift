import SwiftUI
import UIKit

enum Haptics {
    static func impact(_ style: UIImpactFeedbackGenerator.FeedbackStyle) {
        UIImpactFeedbackGenerator(style: style).impactOccurred()
    }
}

/// Un único rodillo con las caras del grupo que termina en el autor. Con movimiento reducido salta al resultado.
struct ReelReveal: View {
    let author: Member
    let onFinish: () -> Void
    @Environment(\.motion) private var motion
    @State private var strip: [Member]
    @State private var offset: CGFloat = 0
    @State private var started = false

    private let cell: CGFloat = 254

    init(author: Member, others: [Member], onFinish: @escaping () -> Void) {
        self.author = author
        self.onFinish = onFinish
        let pool = others.filter { $0.id != author.id }
        var sequence: [Member] = []
        while !pool.isEmpty && sequence.count < 8 { sequence += pool.shuffled() }
        sequence.append(author)
        _strip = State(initialValue: sequence)
    }

    var body: some View {
        Machine(lights: true) {
            VStack(spacing: 0) {
                ForEach(Array(strip.enumerated()), id: \.offset) { _, member in
                    PlayerFace(name: member.name, color: member.color, photo: member.photo, radius: Space.r1)
                        .padding(8)
                        .frame(height: cell)
                }
            }
            .offset(y: offset)
            .frame(height: cell, alignment: .top)
            .clipped()
        }
        .accessibilityHidden(true)
        .onAppear(perform: start)
    }

    private func start() {
        guard !started else { return }
        started = true
        let final = -CGFloat(strip.count - 1) * cell
        if motion.reduced {
            offset = final
            Haptics.impact(.heavy)
            onFinish()
            return
        }
        withAnimation(motion.reel) { offset = final }
        for delay in [0.2, 0.42, 0.66, 0.92, 1.2, 1.45] {
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) { Haptics.impact(.light) }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.55) {
            Haptics.impact(.heavy)
            onFinish()
        }
    }
}

private struct ConfettiPiece: Identifiable {
    let id: Int
    let x: CGFloat
    let color: Color
    let duration: Double
    let delay: Double
}

private struct ConfettiView: View {
    @State private var fall = false
    @State private var pieces: [ConfettiPiece] = (0..<24).map { index in
        ConfettiPiece(
            id: index,
            x: CGFloat.random(in: 0...1),
            color: [Palette.mint, Palette.yellow, Palette.coral, Palette.white][index % 4],
            duration: Double.random(in: 1.2...2.4),
            delay: Double.random(in: 0...0.4)
        )
    }

    var body: some View {
        GeometryReader { proxy in
            ForEach(pieces) { piece in
                Rectangle()
                    .fill(piece.color)
                    .frame(width: 22, height: 11)
                    .rotationEffect(.degrees(fall ? 620 : 0))
                    .position(x: piece.x * proxy.size.width, y: fall ? proxy.size.height + 40 : -24)
                    .animation(.linear(duration: piece.duration).delay(piece.delay), value: fall)
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .onAppear { fall = true }
    }
}

private struct ConfettiOverlay: ViewModifier {
    let active: Bool
    @Environment(\.motion) private var motion

    func body(content: Content) -> some View {
        content.overlay {
            if active && !motion.reduced { ConfettiView() }
        }
    }
}

extension View {
    /// Confeti sobre la vista mientras `active`; nada con movimiento reducido.
    func confetti(_ active: Bool) -> some View { modifier(ConfettiOverlay(active: active)) }
}
