import Auth
import Observation
import PhotosUI
import SwiftUI
import Supabase

/// Rich profile — mirrors Android `UserProfileScreen` (avatar, role, sign out).
struct UserProfileView: View {
    @Environment(Club360AuthSession.self) private var auth: Club360AuthSession
    @State private var pickerItem: PhotosPickerItem?
    @State private var isUploadingAvatar = false
    @State private var uploadError: String?

    var body: some View {
        ZStack {
            Club360ScreenBackground()

            ScrollView {
                VStack(spacing: 22) {
                    PhotosPicker(selection: $pickerItem, matching: .images) {
                        ZStack(alignment: .bottomTrailing) {
                            avatarView
                                .frame(width: 120, height: 120)
                                .clipShape(Circle())
                                .overlay(
                                    Circle()
                                        .stroke(
                                            LinearGradient(
                                                colors: [Club360Theme.teal.opacity(0.8), Club360Theme.purpleLight.opacity(0.6)],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            ),
                                            lineWidth: 2.5
                                        )
                                )

                            Circle()
                                .fill(Club360Theme.primaryButtonGradient)
                                .frame(width: 36, height: 36)
                                .overlay {
                                    if isUploadingAvatar {
                                        ProgressView()
                                            .tint(.white)
                                            .scaleEffect(0.8)
                                    } else {
                                        Image(systemName: "camera.fill")
                                            .font(.caption)
                                            .foregroundStyle(.white)
                                    }
                                }
                                .shadow(color: Club360Theme.purple.opacity(0.35), radius: 6, y: 3)
                        }
                    }
                    .disabled(isUploadingAvatar)
                    .onChange(of: pickerItem) { _, new in
                        Task { await uploadAvatar(from: new) }
                    }

                    Text("Change photo")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(Club360Theme.burgundy.opacity(0.9))

                    Text(displayName)
                        .font(.title2.bold())
                        .foregroundStyle(Club360Theme.cardTitle)

                    if let email = auth.session?.user.email {
                        Text(email)
                            .font(.body)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    }

                    HStack {
                        Text("Status")
                            .foregroundStyle(Club360Theme.captionOnGlass)
                        Spacer()
                        Text(roleLabel)
                            .fontWeight(.semibold)
                            .foregroundStyle(Club360Theme.burgundy)
                    }
                    .padding(16)
                    .club360Glass()

                    if auth.session?.user.isAdminRole == true {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Coach & admin access")
                                .font(.headline.weight(.semibold))
                                .foregroundStyle(Club360Theme.cardTitle)
                            Text(
                                "New sign-ups are always clients. As an admin, open a member from Clients → use Grant coach access on their hub, or set role in Supabase (Authentication → Users → User metadata). Deploy the set-user-role Edge Function from the repo for the in-app button. The member must sign out and sign in again."
                            )
                            .font(.footnote)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                            .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .club360Glass(cornerRadius: 22)
                    }

                    if let uploadError {
                        Text(uploadError)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }

                    NavigationLink {
                        ChangePasswordView()
                    } label: {
                        Text("Change password")
                    }
                    .buttonStyle(Club360PrimaryGradientButtonStyle())

                    Button("Sign out", role: .destructive) {
                        Task { await auth.signOut() }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 4)
                }
                .padding()
            }
        }
        .preferredColorScheme(.light)
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
    }

    private var avatarView: some View {
        Group {
            if let urlString = avatarURLString, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                    case let .success(img):
                        img.resizable().scaledToFill()
                    default:
                        Image(systemName: "person.fill")
                            .font(.system(size: 48))
                            .foregroundStyle(Club360Theme.teal.opacity(0.45))
                    }
                }
            } else {
                Image(systemName: "person.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(Club360Theme.teal.opacity(0.45))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(
            LinearGradient(
                colors: [Club360Theme.mint.opacity(0.5), Club360Theme.teal.opacity(0.2)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }

    private var avatarURLString: String? {
        guard let meta = auth.session?.user.userMetadata else { return nil }
        if case let .string(s) = meta["avatar_url"] { return s }
        if case let .string(s) = meta["picture"] { return s }
        return nil
    }

    private var displayName: String {
        guard let meta = auth.session?.user.userMetadata else { return "Member" }
        if case let .string(name) = meta["name"], !name.isEmpty { return name }
        if let email = auth.session?.user.email, let local = email.split(separator: "@").first {
            return String(local)
        }
        return "Member"
    }

    private var roleLabel: String {
        auth.session?.user.isAdminRole == true ? "Admin" : "Client"
    }

    private func uploadAvatar(from item: PhotosPickerItem?) async {
        guard let item else { return }
        guard let raw = try? await item.loadTransferable(type: Data.self) else {
            uploadError = "Could not read the photo. Try another image."
            return
        }
        guard let jpeg = Club360AvatarImageProcessing.jpegDataForAvatarUpload(raw) else {
            uploadError = "Could not use this image format. Try a photo from your library."
            return
        }
        guard let uid = auth.session?.user.id.uuidString else { return }
        isUploadingAvatar = true
        uploadError = nil
        defer { isUploadingAvatar = false }
        do {
            let url = try await ClientDataService.uploadUserAvatar(data: jpeg, userId: uid)
            do {
                try await auth.updateUserMetadata(["avatar_url": .string(url.absoluteString)])
            } catch {
                uploadError = "Saved photo, but profile update failed: \(error.localizedDescription)"
                return
            }
            pickerItem = nil
        } catch {
            uploadError = "Upload failed: \(error.localizedDescription)"
        }
    }
}
