import PhotosUI
import SwiftUI
import UIKit

/// Cámara del sistema para el selfie.
struct CameraPicker: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    let onImage: (UIImage) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        if UIImagePickerController.isCameraDeviceAvailable(.front) { picker.cameraDevice = .front }
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {
        context.coordinator.parent = self
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        var parent: CameraPicker

        init(_ parent: CameraPicker) { self.parent = parent }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage { parent.onImage(image) }
            parent.isPresented = false
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.isPresented = false
        }
    }
}

/// Selfie y nombre. Sin foto se juega con las iniciales sobre el color del jugador.
struct ProfileView: View {
    let model: GameModel
    let onDone: () -> Void
    @State private var name: String
    @State private var photo: String?
    @State private var showCamera = false
    @State private var pickerItem: PhotosPickerItem?

    private let hasCamera = UIImagePickerController.isSourceTypeAvailable(.camera)

    init(model: GameModel, onDone: @escaping () -> Void) {
        self.model = model
        self.onDone = onDone
        _name = State(initialValue: model.name)
        _photo = State(initialValue: model.photo)
    }

    private var nameReady: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        ScreenFrame {
            DisplayTitle("Smile for the machine", kind: .title)
                .padding(.top, Space.s2)
            Text("Your face goes on the reels.")
                .font(.spBody)
                .foregroundStyle(Palette.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Space.s1)
            Machine {
                PlayerFace(name: name, color: model.color, photo: photo, radius: 0)
                    .frame(maxWidth: .infinity)
            }
            .frame(maxWidth: 240)
            .frame(maxWidth: .infinity)
            .padding(.vertical, Space.s3)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(photo == nil ? Text("No photo yet") : Text("Your selfie"))

            SPField(title: "Your name", prompt: "Name", text: $name)
                .onChange(of: name) { _, value in
                    if value.count > 30 { name = String(value.prefix(30)) }
                }
            if !nameReady {
                Text("Add your name to continue").font(.spCaption).foregroundStyle(Palette.soft).padding(.top, 6)
            }
            Spacer(minLength: Space.s3)
            actions
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker(isPresented: $showCamera) { photo = encodePhotoOrWarn($0) }
                .ignoresSafeArea()
        }
        .onChange(of: pickerItem) { _, item in
            guard let item else { return }
            Task { @MainActor in
                if let data = try? await item.loadTransferable(type: Data.self), let image = UIImage(data: data) {
                    photo = encodePhotoOrWarn(image)
                }
                pickerItem = nil
            }
        }
    }

    private func encodePhotoOrWarn(_ image: UIImage) -> String? {
        let encoded = encodePhoto(image)
        if encoded == nil { model.showError(String(localized: "That photo didn't work. Try another.")) }
        return encoded ?? photo
    }

    private func finish() {
        model.saveProfile(name: name, photo: photo)
        onDone()
    }

    @ViewBuilder private var actions: some View {
        if photo != nil {
            PrimaryButton("Looks good") { finish() }.disabled(!nameReady)
            if hasCamera {
                LinkButton("Retake") { showCamera = true }
            } else {
                PhotosPicker(selection: $pickerItem, matching: .images) {
                    Text("Retake").font(.spBody).foregroundStyle(Palette.white).frame(maxWidth: .infinity, minHeight: Space.touch)
                }
            }
        } else {
            if hasCamera {
                PrimaryButton("Take photo") { showCamera = true }
                PhotosPicker(selection: $pickerItem, matching: .images) {
                    Text("Choose from library").font(.spBody).foregroundStyle(Palette.white).frame(maxWidth: .infinity, minHeight: Space.touch)
                }
            } else {
                PhotosPicker(selection: $pickerItem, matching: .images) {
                    LipLabel(title: "Choose a photo")
                }
            }
            LinkButton("Use my initials instead") { finish() }.disabled(!nameReady)
        }
    }
}
