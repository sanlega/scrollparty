import Foundation

private let linkPattern = /https:\/\/(([a-z]+\.)?(instagram|tiktok|youtube)\.com|youtu\.be)\/[^\s]+/

/// Saca el enlace de un texto pegado o compartido, que a menudo trae más palabras.
/// Debe coincidir con firestore.rules. Lo comparten la app y la extensión de compartir.
func extractLink(_ text: String) -> String? {
    guard let match = text.firstMatch(of: linkPattern), match.output.0.count <= 500 else { return nil }
    return String(match.output.0)
}
