import SwiftUI

/// La máquina antes de empezar: código, invitar, contador, jugadores y añadir reels.
struct LobbyView: View {
    let model: GameModel
    let group: Party
    @State private var link = ""
    @State private var clue = ""

    private var total: Int { model.members.reduce(0) { $0 + $1.count } }
    private var inviteText: String {
        String(localized: "Join my ScrollParty! Party code: \(group.code)")
    }

    var body: some View {
        ScreenFrame {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Party code").spLabelStyle()
                    Text(verbatim: group.code)
                        .font(.spNumber(26))
                        .tracking(1)
                        .foregroundStyle(Palette.yellow)
                        .textSelection(.enabled)
                }
                Spacer(minLength: Space.s1)
                ShareLink(item: inviteText) {
                    Label("Invite", systemImage: "square.and.arrow.up")
                        .font(.spBody)
                        .foregroundStyle(Palette.ink)
                        .padding(.horizontal, Space.s2)
                        .frame(minHeight: Space.touch)
                        .background(RoundedRectangle(cornerRadius: Space.r1).fill(Palette.white))
                }
            }
            .padding(.top, Space.s1)

            Machine(lights: true) {
                VStack(spacing: 0) {
                    Text(verbatim: "\(total)")
                        .font(.spNumber(96))
                        .foregroundStyle(Palette.blue)
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)
                    Text("reels in the machine").font(.spBody).foregroundStyle(Palette.ink)
                }
                .frame(maxWidth: .infinity, minHeight: 200)
                .padding(.vertical, Space.s1)
                .accessibilityElement(children: .combine)
            }
            .padding(.top, Space.s2)

            HStack {
                Text("\(model.members.count) players").spLabelStyle()
                Spacer()
                Text("reels sent").spLabelStyle()
            }
            .padding(.top, Space.s3)
            .padding(.bottom, Space.s1)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: 10) {
                    ForEach(model.members) { member in
                        VStack(spacing: 2) {
                            PlayerFace(name: member.name, color: member.color, photo: member.photo, size: 58)
                            (member.id == model.uid ? Text("You") : Text(verbatim: member.name))
                                .font(.spCaption)
                                .foregroundStyle(Palette.white)
                                .lineLimit(1)
                            Text(verbatim: "\(member.count)").font(.spCaption).foregroundStyle(Palette.soft)
                        }
                        .frame(width: 64)
                        .accessibilityElement(children: .combine)
                    }
                }
            }

            SPField(title: "Add a reel", prompt: "Paste a reel, TikTok or Short", text: $link)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.URL)
                .padding(.top, Space.s3)
            SPField(title: "Anonymous clue (optional)", prompt: "e.g. my mum sent me this", text: $clue)
                .padding(.top, Space.s2)
                .onChange(of: clue) { _, value in
                    if value.count > 80 { clue = String(value.prefix(80)) }
                }
            PrimaryButton("Add to the machine", style: .alt) {
                guard extractLink(link) != nil else {
                    model.showError(String(localized: "Paste an Instagram, TikTok or YouTube link"))
                    return
                }
                model.addLink(link, clue: clue)
                link = ""
                clue = ""
            }
            .disabled(link.trimmingCharacters(in: .whitespaces).isEmpty)
            .padding(.top, Space.s2)

            if !model.myLinks.isEmpty {
                Text("Your reels").spLabelStyle().padding(.top, Space.s2)
                ForEach(model.myLinks) { item in
                    HStack {
                        Text(verbatim: item.url).font(.spCaption).foregroundStyle(Palette.white).lineLimit(1)
                        Spacer()
                        Button { model.removeLink(item) } label: {
                            Text("Remove").font(.spBody).foregroundStyle(Palette.yellow)
                                .frame(minWidth: Space.touch, minHeight: Space.touch)
                        }
                    }
                }
            }

            Spacer(minLength: Space.s3)
            if model.isAdmin {
                PrimaryButton("Start the party") { model.start() }
                    .disabled(model.members.count < 2 || total == 0)
            } else {
                Text("Waiting for the host to start…")
                    .font(.spBody)
                    .foregroundStyle(Palette.soft)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, Space.s2)
            }
            LinkButton("Leave party") { model.forget() }
        }
    }
}
