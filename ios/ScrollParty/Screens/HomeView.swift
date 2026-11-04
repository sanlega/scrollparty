import SwiftUI

/// Inicio: crear una fiesta o unirse con el código.
struct HomeView: View {
    let model: GameModel
    let onEditProfile: () -> Void
    @State private var code = ""
    @State private var busy = false

    var body: some View {
        ScreenFrame {
            DisplayTitle(verbatim: "ScrollParty")
                .padding(.top, Space.s3)
            Text("Feed the machine with your best reels. Guess who sent what.")
                .font(.spBody)
                .foregroundStyle(Palette.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Space.s1)

            HStack(spacing: Space.s2) {
                PlayerFace(name: model.name, color: model.color, photo: model.photo, size: 56)
                Text(verbatim: model.name).font(.spHeading).foregroundStyle(Palette.white).lineLimit(1).minimumScaleFactor(0.5)
                Spacer(minLength: 0)
                Button(action: onEditProfile) {
                    Text("Edit").font(.spBody).foregroundStyle(Palette.yellow)
                        .frame(minWidth: Space.touch, minHeight: Space.touch)
                }
            }
            .padding(.top, Space.s3)

            Spacer(minLength: Space.s3)
            PrimaryButton("Create a party", loading: busy) {
                busy = true
                model.create()
            }
            SPField(title: "Party code", prompt: "ABC123", text: $code)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .padding(.top, Space.s2)
                .onChange(of: code) { _, value in
                    let cleaned = String(value.uppercased().filter { $0.isLetter || $0.isNumber }.prefix(6))
                    if cleaned != value { code = cleaned }
                }
            PrimaryButton("Join a party", style: .alt) {
                busy = true
                model.joinGroup(code: code)
            }
            .disabled(code.count != 6)
            .padding(.top, Space.s2)
        }
        .onChange(of: model.toast) { _, _ in busy = false }
    }
}
