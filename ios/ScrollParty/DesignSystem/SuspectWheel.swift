import SwiftUI

/// Rueda de sospechosos: scroll con imán, tocar centra y selecciona. La barra coral marca el centrado.
struct SuspectWheel: View {
    let members: [Member]
    @Binding var centered: String?
    @Environment(\.motion) private var motion

    private let rowHeight: CGFloat = 72
    private let wheelHeight: CGFloat = 310

    var body: some View {
        Machine {
            ZStack {
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 0) {
                        ForEach(members) { member in row(member) }
                    }
                    .scrollTargetLayout()
                }
                .contentMargins(.vertical, (wheelHeight - rowHeight) / 2, for: .scrollContent)
                .scrollTargetBehavior(.viewAligned)
                .scrollPosition(id: $centered)
                .sensoryFeedback(.selection, trigger: centered)

                VStack(spacing: 0) {
                    LinearGradient(colors: [Palette.white, Palette.white.opacity(0)], startPoint: .top, endPoint: .bottom)
                        .frame(height: 46)
                    Spacer(minLength: 0)
                    LinearGradient(colors: [Palette.white.opacity(0), Palette.white], startPoint: .top, endPoint: .bottom)
                        .frame(height: 46)
                }
                .allowsHitTesting(false)

                VStack(spacing: 0) {
                    Rectangle().fill(Palette.coral).frame(height: 4)
                    Spacer(minLength: 0)
                    Rectangle().fill(Palette.coral).frame(height: 4)
                }
                .frame(height: rowHeight + 4)
                .allowsHitTesting(false)
            }
            .frame(height: wheelHeight)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text("Choose who sent it"))
    }

    private func row(_ member: Member) -> some View {
        let isCentered = centered == member.id
        return Button {
            withAnimation(motion.snap) { centered = member.id }
        } label: {
            HStack(spacing: Space.s2) {
                PlayerFace(name: member.name, color: member.color, photo: member.photo, size: 56)
                    .scaleEffect(isCentered ? 1.06 : 1)
                Text(verbatim: member.name)
                    .font(.spHeading)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .foregroundStyle(isCentered ? Palette.ink : Palette.inkSoft)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, Space.s2)
            .frame(height: rowHeight)
            .scaleEffect(isCentered ? 1.04 : 1)
            .animation(motion.spring, value: isCentered)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isCentered ? .isSelected : [])
    }
}
