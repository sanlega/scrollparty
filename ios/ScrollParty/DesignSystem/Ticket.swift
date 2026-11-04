import SwiftUI

/// Rectángulo con muescas laterales, como un tique de feria.
private struct TicketShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.addRoundedRect(in: rect, cornerSize: CGSize(width: 6, height: 6))
        path.addEllipse(in: CGRect(x: rect.minX - 8, y: rect.midY - 8, width: 16, height: 16))
        path.addEllipse(in: CGRect(x: rect.maxX - 8, y: rect.midY - 8, width: 16, height: 16))
        return path
    }
}

/// Tique mint (acierto, puntos) o blush (`fooled`: engañado). Texto siempre `ink`.
struct Ticket: View {
    let text: Text
    let fooled: Bool
    let small: Bool

    init(_ text: Text, fooled: Bool = false, small: Bool = false) {
        self.text = text
        self.fooled = fooled
        self.small = small
    }

    var body: some View {
        text
            .font(small ? .spLabel : .spTicket)
            .foregroundStyle(Palette.ink)
            .multilineTextAlignment(.center)
            .padding(.horizontal, small ? 18 : 24)
            .padding(.vertical, small ? 7 : 10)
            .background(
                TicketShape().fill(fooled ? Palette.blush : Palette.mint, style: FillStyle(eoFill: true))
            )
    }
}
