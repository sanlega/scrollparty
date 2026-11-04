import SwiftUI

@main
struct ScrollPartyApp: App {
    var body: some Scene {
        WindowGroup {
            // Los tests lógicos no deben arrancar Firebase en la app anfitriona.
            if NSClassFromString("XCTestCase") == nil {
                root
                    .motionScope()
                    .preferredColorScheme(.dark)
                    .tint(Palette.blue)
            }
        }
    }

    @ViewBuilder private var root: some View {
        #if DEBUG
        if let screen = UserDefaults.standard.string(forKey: "demoScreen") {
            DemoRoot(screen: screen)
        } else {
            RootView()
        }
        #else
        RootView()
        #endif
    }
}

struct RootView: View {
    @State private var model = GameModel()

    var body: some View {
        Router(model: model)
    }
}

/// Elige la pantalla según el estado de la partida y el rol de este móvil.
struct Router: View {
    let model: GameModel
    var demo: String?
    @AppStorage("onboardingDone") private var onboardingDone = false
    @State private var editingProfile = false

    private var needsOnboarding: Bool { demo == "onboarding" || (demo == nil && !onboardingDone) }
    private var needsProfile: Bool {
        demo == "selfie" || (demo == nil && (model.name.isEmpty || editingProfile))
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            screen
            if let toast = model.toast {
                Toast(message: toast)
                    .padding(.horizontal, Space.margin)
                    .padding(.bottom, Space.s3)
                    .transition(.opacity)
                    .task(id: toast.id) {
                        try? await Task.sleep(nanoseconds: 3_500_000_000)
                        if model.toast?.id == toast.id { model.toast = nil }
                    }
            }
        }
        .animation(.easeInOut(duration: 0.15), value: model.toast?.id)
    }

    @ViewBuilder private var screen: some View {
        if needsOnboarding {
            OnboardingView { onboardingDone = true }
        } else if model.uid == nil {
            ZStack {
                SunBackground()
                ProgressView().tint(Palette.white)
            }
        } else if let group = model.group {
            switch group.status {
            case "collecting": LobbyView(model: model, group: group)
            case "playing": session(group)
            default: FinalView(model: model)
            }
        } else if needsProfile {
            ProfileView(model: model) { editingProfile = false }
        } else {
            HomeView(model: model) { editingProfile = true }
        }
    }

    @ViewBuilder private func session(_ group: Party) -> some View {
        if let round = model.currentRound {
            if round.authorId != nil {
                RevealView(model: model, group: group, round: round).id(round.id)
            } else if model.isAdmin {
                StageView(model: model, group: group, round: round, openSecret: demo == "secret").id(round.id)
            } else if model.myLinks.contains(where: { $0.id == round.id }) {
                YoursView(model: model, round: round).id(round.id)
            } else {
                PickView(model: model, group: group, round: round).id(round.id)
            }
        } else {
            ScreenFrame {
                Spacer()
                DisplayTitle("Getting the machine ready…", kind: .title)
                Spacer()
                if model.isAdmin {
                    PrimaryButton("Retry") { model.start() }
                }
            }
        }
    }
}
