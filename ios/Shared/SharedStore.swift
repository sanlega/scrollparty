import Foundation
import Security

/// Lo que la app le pasa a la extensión de compartir al unirse a un grupo.
struct SharedParty: Codable, Equatable {
    let code: String
    let refreshToken: String
    let apiKey: String
    let projectId: String
    let uid: String
}

/// Grupo de llavero compartido entre la app y la extensión (no se usan App Groups: la CI no puede crearlos).
enum SharedStore {
    private static var accessGroup: String? {
        guard let prefix = Bundle.main.object(forInfoDictionaryKey: "AppIdentifierPrefix") as? String, !prefix.isEmpty else { return nil }
        return prefix + "com.santilega.scrollparty.shared"
    }

    private static func query() -> [String: Any]? {
        guard let group = accessGroup else { return nil }
        return [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "scrollparty.share",
            kSecAttrAccount as String: "party",
            kSecAttrAccessGroup as String: group,
        ]
    }

    static func save(_ party: SharedParty) {
        guard var attrs = query(), let data = try? JSONEncoder().encode(party) else { return }
        SecItemDelete(attrs as CFDictionary)
        attrs[kSecValueData as String] = data
        attrs[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(attrs as CFDictionary, nil)
    }

    static func load() -> SharedParty? {
        guard var attrs = query() else { return nil }
        attrs[kSecReturnData as String] = true
        attrs[kSecMatchLimit as String] = kSecMatchLimitOne
        var out: CFTypeRef?
        guard SecItemCopyMatching(attrs as CFDictionary, &out) == errSecSuccess, let data = out as? Data else { return nil }
        return try? JSONDecoder().decode(SharedParty.self, from: data)
    }

    static func clear() {
        guard let attrs = query() else { return }
        SecItemDelete(attrs as CFDictionary)
    }
}
