import SwiftUI
import UIKit

struct ToastMessage: Equatable, Identifiable {
    let id = UUID()
    var title: String
    var subtitle: String?
}

/// Aviso amarillo de la máquina: errores y confirmaciones.
struct Toast: View {
    let message: ToastMessage

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(verbatim: message.title).font(.spBody)
            if let subtitle = message.subtitle {
                Text(verbatim: subtitle).font(.spCaption)
            }
        }
        .foregroundStyle(Palette.ink)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .padding(.vertical, Space.s2)
        .background(
            RoundedRectangle(cornerRadius: Space.r1)
                .fill(Palette.yellow)
                .shadow(color: Palette.yellowDeep, radius: 0, x: 0, y: 7)
        )
        .padding(.bottom, 7)
        .accessibilityElement(children: .combine)
        .onAppear { UIAccessibility.post(notification: .announcement, argument: message.title) }
    }
}
