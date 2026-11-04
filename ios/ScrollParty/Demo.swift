#if DEBUG
import SwiftUI
import UIKit

/// Modo de demostración (solo DEBUG): `-demoScreen <onboarding|selfie|lobby|stage|secret|pick|yours|reveal|final>`.
/// Enseña esa pantalla con datos falsos y sin red. Lo usa la CI para hacer capturas.
struct DemoRoot: View {
    let screen: String
    @State private var model: GameModel

    init(screen: String) {
        self.screen = screen
        _model = State(initialValue: GameModel.demo(screen))
    }

    var body: some View {
        Router(model: model, demo: screen)
    }
}

/// Selfie falso: fondo gris, cuerpo y cabeza.
private func demoPhoto(_ shade: CGFloat) -> String? {
    let size = CGSize(width: 256, height: 256)
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    let image = UIGraphicsImageRenderer(size: size, format: format).image { context in
        UIColor(white: 0.85, alpha: 1).setFill()
        context.fill(CGRect(origin: .zero, size: size))
        UIColor(white: shade, alpha: 1).setFill()
        context.cgContext.fillEllipse(in: CGRect(x: 30, y: 170, width: 196, height: 160))
        UIColor(white: 0.97, alpha: 1).setFill()
        context.cgContext.fillEllipse(in: CGRect(x: 78, y: 50, width: 100, height: 112))
    }
    return encodePhoto(image)
}

extension GameModel {
    static func demo(_ screen: String) -> GameModel {
        let model = GameModel(demo: true)
        let me = "u5"
        model.uid = me
        model.members = [
            Member(id: "u1", name: "Ana", color: 0, photo: demoPhoto(0.2), count: 3, joinedAt: 1),
            Member(id: "u2", name: "Leo", color: 1, photo: demoPhoto(0.4), count: 2, joinedAt: 2),
            Member(id: "u3", name: "Marta", color: 2, photo: nil, count: 1, joinedAt: 3),
            Member(id: "u4", name: "Nico", color: 3, photo: demoPhoto(0.1), count: 2, joinedAt: 4),
            Member(id: "u5", name: "Iker", color: 5, photo: demoPhoto(0.3), count: 3, joinedAt: 5),
            Member(id: "u6", name: "Sara", color: 4, photo: nil, count: 1, joinedAt: 6),
        ]
        let url = "https://www.instagram.com/reel/demo/"
        let ids = ["r1", "r2", "r3", "r4", "r5"]
        // Rondas ya reveladas, para el final y para «Reel of the Night».
        var rounds = [
            Round(id: "r1", url: url, authorId: "u1", votes: ["u2": "u1", "u3": "u4", "u4": "u1", "u5": "u1", "u6": "u2"],
                  clue: "", reactions: ["u2": "laugh", "u3": "skull", "u4": "laugh"]),
            Round(id: "r2", url: url, authorId: "u2", votes: ["u1": "u2", "u3": "u2", "u4": "u1", "u5": "u2", "u6": "u2"],
                  clue: "", reactions: ["u1": "fire"]),
            Round(id: "r3", url: url, authorId: "u3", votes: ["u1": "u3", "u2": "u1", "u4": "u3", "u5": "u6", "u6": "u3"],
                  clue: "", reactions: [:]),
            Round(id: "r4", url: url, authorId: nil, votes: ["u1": "u2", "u2": "u1", "u3": "u2"],
                  clue: "my mum sent me this", reactions: ["u1": "laugh", "u3": "fire"]),
            Round(id: "r5", url: url, authorId: "u5", votes: ["u1": "u5", "u2": "u4", "u3": "u5", "u4": "u5", "u6": "u1"],
                  clue: "", reactions: ["u2": "skull"]),
        ]
        var adminId = "u1"
        var status = "playing"
        var index = 3
        switch screen {
        case "lobby":
            status = "collecting"
            adminId = me
            model.myLinks = [Link(id: "l1", url: url), Link(id: "l2", url: "https://youtu.be/demo")]
        case "stage", "secret":
            adminId = me
        case "yours":
            model.myLinks = [Link(id: "r4", url: url)]
        case "reveal":
            adminId = me
            rounds[3] = Round(
                id: "r4", url: url, authorId: "u2", votes: ["u1": "u2", "u2": "u1", "u3": "u4", "u4": "u2", "u5": "u2", "u6": "u3"],
                clue: "my mum sent me this", reactions: ["u1": "laugh"]
            )
        case "final":
            status = "finished"
            rounds[3] = Round(
                id: "r4", url: url, authorId: "u2", votes: ["u1": "u2", "u3": "u4", "u4": "u2", "u5": "u2", "u6": "u3"],
                clue: "", reactions: ["u1": "laugh", "u3": "laugh", "u4": "skull"]
            )
            index = 4
        default:
            break
        }
        model.rounds = rounds
        if screen != "onboarding" && screen != "selfie" {
            model.group = Party(code: "JY3SHV", adminId: adminId, status: status, roundIds: ids, roundIndex: index)
        }
        return model
    }
}
#endif
