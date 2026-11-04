import SwiftUI
import UIKit

private let faceCache = NSCache<NSString, UIImage>()

private func decodePhoto(_ photo: String) -> UIImage? {
    let key = String(photo.hashValue) as NSString
    if let hit = faceCache.object(forKey: key) { return hit }
    guard let data = Data(base64Encoded: photo), let image = UIImage(data: data) else { return nil }
    faceCache.setObject(image, forKey: key)
    return image
}

/// Trama de puntos que une cualquier selfie con el color del jugador.
private struct DotPattern: View {
    var body: some View {
        Canvas { context, size in
            var path = Path()
            var y: CGFloat = 2
            while y < size.height {
                var x: CGFloat = 2
                while x < size.width {
                    path.addEllipse(in: CGRect(x: x - 0.8, y: y - 0.8, width: 1.6, height: 1.6))
                    x += 4
                }
                y += 4
            }
            context.fill(path, with: .color(Palette.ink.opacity(0.28)))
        }
        .allowsHitTesting(false)
    }
}

/// Selfie a dos tonos (gris × color del jugador + puntos) o iniciales en `ink` sobre el color.
struct PlayerFace: View {
    let name: String
    let color: Int
    var photo: String?
    var size: CGFloat?
    var radius: CGFloat = Space.r1

    var body: some View {
        let tint = Palette.player(color)
        ZStack {
            if let photo, let image = decodePhoto(photo) {
                Color.clear.overlay(
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .grayscale(1)
                        .colorMultiply(tint)
                )
                .clipped()
                DotPattern()
            } else {
                tint
                Text(verbatim: initials(name))
                    .font(.spHeading)
                    .minimumScaleFactor(0.4)
                    .lineLimit(1)
                    .foregroundStyle(Palette.ink)
                    .padding(4)
            }
        }
        .frame(width: size, height: size)
        .aspectRatio(1, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: radius))
        .accessibilityHidden(true)
    }
}
