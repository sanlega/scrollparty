import Foundation

struct Group: Equatable {
    let code: String
    let adminId: String
    let status: String
    let roundIds: [String]
    let roundIndex: Int
}

struct Member: Identifiable, Equatable {
    let id: String
    let name: String
    let emoji: String
    let count: Int
    let joinedAt: Int64
}

struct Link: Identifiable, Equatable {
    let id: String
    let url: String
}

struct Round: Identifiable, Equatable {
    let id: String
    let url: String
    let authorId: String?
    let votes: [String: String]
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

private let linkPattern = /https:\/\/(([a-z]+\.)?(instagram|tiktok|youtube)\.com|youtu\.be)\/[^\s]+/

/// Saca el enlace de un texto pegado o compartido, que a menudo trae más palabras. Debe coincidir con firestore.rules.
func extractLink(_ text: String) -> String? {
    guard let match = text.firstMatch(of: linkPattern), match.output.0.count <= 500 else { return nil }
    return String(match.output.0)
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
