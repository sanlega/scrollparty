import SwiftUI

private struct StepArt: View {
    let step: Int
    @Environment(\.motion) private var motion
    @State private var count = 0

    var body: some View {
        Group {
            switch step {
            case 0: machineArt
            case 1: tvArt
            case 2: suspectArt
            default: appArt
            }
        }
        .accessibilityHidden(true)
    }

    private var machineArt: some View {
        VStack(spacing: Space.s2) {
            Machine(lights: true) {
                Text(verbatim: "\(count)")
                    .font(.spNumber(64))
                    .foregroundStyle(Palette.blue)
                    .contentTransition(.numericText())
                    .frame(maxWidth: .infinity, minHeight: 140)
            }
            .frame(maxWidth: 250)
            Text("reels in the machine").spLabelStyle(Palette.white)
        }
        .task {
            if motion.reduced {
                count = 11
                return
            }
            for value in 1...11 {
                try? await Task.sleep(nanoseconds: 80_000_000)
                withAnimation { count = value }
            }
        }
    }

    private var tvArt: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(Palette.night)
            .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Palette.white, lineWidth: 7))
            .frame(maxWidth: 290)
            .frame(height: 180)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .fill(LinearGradient(colors: [Color(hex: 0xFF6A5F), Color(hex: 0xA331E0), Color(hex: 0x20124A)], startPoint: .top, endPoint: .bottom))
                    .frame(width: 88, height: 142)
                    .overlay(Image(systemName: "play.fill").font(.title).foregroundStyle(Palette.white))
                    .pop()
            )
    }

    private var suspectArt: some View {
        VStack(spacing: Space.s3) {
            Machine {
                HStack(spacing: Space.s2) {
                    PlayerFace(name: "Leo", color: 1, size: 62)
                    Text(verbatim: "Leo").font(.spHeading).foregroundStyle(Palette.ink)
                    Spacer(minLength: 0)
                }
                .padding(10)
            }
            .frame(maxWidth: 250)
            Ticket(Text("+1 ticket")).pop()
        }
    }

    private var appArt: some View {
        RoundedRectangle(cornerRadius: 26)
            .fill(Palette.blue)
            .overlay(RoundedRectangle(cornerRadius: 26).strokeBorder(Palette.yellow, lineWidth: 4))
            .overlay(Text(verbatim: "S").font(.spNumber(52)).foregroundStyle(Palette.yellow))
            .frame(width: 118, height: 118)
            .pop()
    }
}

/// Cuatro pasos antes del primer uso; se puede saltar.
struct OnboardingView: View {
    let onDone: () -> Void
    @Environment(\.motion) private var motion
    @State private var step = 0

    private let titles: [LocalizedStringKey] = ["Feed the machine", "It plays on your TV", "Scroll to your suspect", "One tap from any app"]
    private let bodies: [LocalizedStringKey] = [
        "Share any reel, TikTok or Short to the party. Nobody sees what you sent.",
        "Mirror the host’s phone to the TV. Reels play one by one, in random order.",
        "Who sent it? Pick on your own phone. Right guesses and fooled friends win tickets.",
        "See something good? Tap Share, then ScrollParty. It lands in your party instantly.",
    ]
    private var last: Bool { step == titles.count - 1 }

    var body: some View {
        ScreenFrame {
            HStack(spacing: Space.s2) {
                StepBar(step: step, total: titles.count)
                Button(action: onDone) {
                    Text("Skip")
                        .font(.spBody)
                        .foregroundStyle(Palette.white)
                        .frame(minWidth: Space.touch, minHeight: Space.touch)
                }
            }
            Spacer(minLength: Space.s2)
            StepArt(step: step)
                .id(step)
                .frame(maxWidth: .infinity, minHeight: 230)
            Spacer(minLength: Space.s2)
            DisplayTitle(titles[step], alignment: .leading)
                .id("t\(step)")
                .padding(.top, Space.s1)
            Text(bodies[step])
                .font(.spBody)
                .foregroundStyle(Palette.white)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, Space.s2)
            PrimaryButton(last ? "Get started" : "Next") {
                if last {
                    onDone()
                } else {
                    withAnimation(motion.snap) { step += 1 }
                }
            }
            .padding(.top, Space.s3)
        }
    }
}
