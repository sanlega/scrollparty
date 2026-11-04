import Foundation
import UIKit

struct Party: Equatable {
    let code: String
    let adminId: String
    let status: String
    let roundIds: [String]
    let roundIndex: Int
}

struct Member: Identifiable, Equatable {
    let id: String
    var name: String
    var color: Int = 0
    var photo: String? = nil
    var count: Int = 0
    var joinedAt: Int64 = 0
}

struct Link: Identifiable, Equatable {
    let id: String
    let url: String
}

enum Reaction: String, CaseIterable {
    case laugh, skull, fire, sleep

    var emoji: String {
        switch self {
        case .laugh: return "😂"
        case .skull: return "💀"
        case .fire: return "🔥"
        case .sleep: return "😴"
        }
    }
}

struct Round: Identifiable, Equatable {
    let id: String
    let url: String
    let authorId: String?
    let votes: [String: String]
    var clue: String = ""
    var reactions: [String: String] = [:]
}

struct Standing: Equatable {
    let memberId: String
    let correct: Int
    let fooled: Int
    var score: Int { correct + fooled }
}

/// Sin I, O, 0 ni 1 para que el código se pueda dictar en voz alta.
private let codeAlphabet = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")

func newCode() -> String {
    String((0..<6).map { _ in codeAlphabet.randomElement()! })
}

let playerColorCount = 12

/// Color (0-11) de `me`: el preferido si no lo usa nadie que entró antes; si no, el primero libre.
func assignColor(me: Member, preferred: Int, members: [Member]) -> Int {
    let taken = Set(members.filter { $0.id != me.id && ($0.joinedAt, $0.id) < (me.joinedAt, me.id) }.map(\.color))
    let want = (0..<playerColorCount).contains(preferred) ? preferred : 0
    if !taken.contains(want) { return want }
    return (0..<playerColorCount).first { !taken.contains($0) } ?? want
}

/// +1 por acertar el autor; +1 al autor por cada amigo que falla.
func standings(rounds: [Round], memberIds: [String]) -> [Standing] {
    var correct: [String: Int] = [:]
    var fooled: [String: Int] = [:]
    for round in rounds {
        guard let author = round.authorId else { continue }
        for (voter, guess) in round.votes where voter != author {
            if guess == author {
                correct[voter, default: 0] += 1
            } else {
                fooled[author, default: 0] += 1
            }
        }
    }
    return memberIds
        .map { Standing(memberId: $0, correct: correct[$0] ?? 0, fooled: fooled[$0] ?? 0) }
        .enumerated()
        .sorted { ($0.element.score, $1.offset) > ($1.element.score, $0.offset) }
        .map(\.element)
}

/// Los premiados: todos los empatados en el máximo; nadie si el máximo es 0.
func winners(_ table: [Standing], by value: (Standing) -> Int) -> [String] {
    let best = table.map(value).max() ?? 0
    return best == 0 ? [] : table.filter { value($0) == best }.map(\.memberId)
}

/// Autor de la ronda con más reacciones laugh + skull; en empate, la primera. Nadie si ninguna tuvo.
/// `rounds` debe venir en el orden de juego.
func reelOfTheNight(rounds: [Round]) -> String? {
    var best = 0
    var author: String?
    for round in rounds {
        guard let a = round.authorId else { continue }
        let n = round.reactions.values.filter { $0 == Reaction.laugh.rawValue || $0 == Reaction.skull.rawValue }.count
        if n > best {
            best = n
            author = a
        }
    }
    return author
}

func initials(_ name: String) -> String {
    let parts = name.split(separator: " ").prefix(2)
    return parts.compactMap { $0.first.map { String($0).uppercased() } }.joined()
}

let maxPhotoChars = 40000

/// Selfie → cuadrado de 256 px en JPEG base64, bajando la calidad hasta que quepa en 40 000 caracteres.
func encodePhoto(_ image: UIImage) -> String? {
    let side: CGFloat = 256
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    format.opaque = true
    let size = image.size
    guard size.width > 0, size.height > 0 else { return nil }
    let scale = max(side / size.width, side / size.height)
    let drawn = CGSize(width: size.width * scale, height: size.height * scale)
    let square = UIGraphicsImageRenderer(size: CGSize(width: side, height: side), format: format).image { _ in
        image.draw(in: CGRect(x: (side - drawn.width) / 2, y: (side - drawn.height) / 2, width: drawn.width, height: drawn.height))
    }
    var quality: CGFloat = 0.8
    while quality >= 0.1 {
        if let data = square.jpegData(compressionQuality: quality) {
            let text = data.base64EncodedString()
            if text.count <= maxPhotoChars { return text }
        }
        quality -= 0.1
    }
    return nil
}
