import SwiftUI

extension Reaction {
    var label: LocalizedStringKey {
        switch self {
        case .laugh: return "Laugh"
        case .skull: return "Dead"
        case .fire: return "Fire"
        case .sleep: return "Boring"
        }
    }
}

private struct ReactionStyle: ButtonStyle {
    let selected: Bool

    func makeBody(configuration: Configuration) -> some View {
        ReactionFace(selected: selected, pressed: configuration.isPressed, label: configuration.label)
    }
}

private struct ReactionFace<Face: View>: View {
    let selected: Bool
    let pressed: Bool
    let label: Face
    @Environment(\.motion) private var motion

    var body: some View {
        label
            .frame(maxWidth: .infinity, minHeight: 58)
            .background(
                RoundedRectangle(cornerRadius: Space.r1)
                    .fill(Palette.white)
                    .shadow(color: Palette.lipAlt, radius: 0, x: 0, y: 4)
            )
            .overlay(
                RoundedRectangle(cornerRadius: Space.r1)
                    .strokeBorder(Palette.coral, lineWidth: selected ? 4 : 0)
            )
            .scaleEffect(pressed && !motion.reduced ? 1.2 : 1)
            .animation(motion.spring, value: pressed)
            .padding(.bottom, 4)
    }
}

/// Cuatro reacciones en vivo para la tele. Una por jugador y ronda: cambiarla la vuelve a lanzar.
struct ReactionBar: View {
    var selected: Reaction?
    let onReact: (Reaction) -> Void

    var body: some View {
        HStack(spacing: Space.s1) {
            ForEach(Reaction.allCases, id: \.self) { reaction in
                Button {
                    Haptics.impact(.light)
                    onReact(reaction)
                } label: {
                    Text(reaction.emoji).font(.title)
                }
                .buttonStyle(ReactionStyle(selected: selected == reaction))
                .accessibilityLabel(Text(reaction.label))
                .accessibilityAddTraits(selected == reaction ? .isSelected : [])
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text("React live on the TV"))
    }
}

private struct Flyer: Identifiable {
    let id = UUID()
    let emoji: String
    let x: CGFloat
}

private struct FlyerView: View {
    let flyer: Flyer
    let onDone: () -> Void
    @State private var go = false

    var body: some View {
        Text(flyer.emoji)
            .font(.system(size: 40))
            .rotationEffect(.degrees(go ? 16 : 0))
            .scaleEffect(go ? 1 : 0.6)
            .offset(y: go ? -300 : 16)
            .opacity(go ? 0 : 1)
            .animation(.easeOut(duration: 1.6), value: go)
            .padding(.leading, flyer.x)
            .padding(.bottom, 190)
            .onAppear {
                go = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.7) { onDone() }
            }
    }
}

/// En la tele: lanza las reacciones que aparecen o cambian. Con movimiento reducido, un contador.
struct FlyingReactions: View {
    let reactions: [String: String]
    @Environment(\.motion) private var motion
    @State private var flyers: [Flyer] = []

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .bottomLeading) {
                if motion.reduced {
                    tally
                } else {
                    ForEach(flyers) { flyer in
                        FlyerView(flyer: flyer) { flyers.removeAll { $0.id == flyer.id } }
                    }
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height, alignment: .bottomLeading)
            .onChange(of: reactions) { old, new in launch(old: old, new: new, width: proxy.size.width) }
        }
        .allowsHitTesting(false)
    }

    private func launch(old: [String: String], new: [String: String], width: CGFloat) {
        guard !motion.reduced else { return }
        for (uid, value) in new where old[uid] != value {
            guard let reaction = Reaction(rawValue: value) else { continue }
            flyers.append(Flyer(emoji: reaction.emoji, x: CGFloat.random(in: 0...max(1, width - 60))))
        }
    }

    private var tally: some View {
        HStack(spacing: Space.s1) {
            ForEach(Reaction.allCases, id: \.self) { reaction in
                let count = reactions.values.filter { $0 == reaction.rawValue }.count
                if count > 0 {
                    Text(verbatim: "\(reaction.emoji) \(count)")
                        .font(.spBody)
                        .foregroundStyle(Palette.ink)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Capsule().fill(Palette.white))
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}
