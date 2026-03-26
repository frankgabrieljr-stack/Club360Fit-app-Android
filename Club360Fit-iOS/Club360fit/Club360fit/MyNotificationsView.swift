import Observation
import SwiftUI

/// Member inbox uses `read_at`; coach inbox (viewing a client) uses `coach_read_at` so each side clears independently.
enum MyNotificationsInboxKind: Sendable {
    case member
    case coach
}

/// Mirrors Android `MyNotificationsScreen`.
struct MyNotificationsView: View {
    var inbox: MyNotificationsInboxKind = .member

    @Environment(ClientHomeViewModel.self) private var home: ClientHomeViewModel
    @Environment(\.clientTabRouter) private var tabRouter
    @Environment(\.dismiss) private var dismiss
    @State private var model = MyNotificationsViewModel()

    var body: some View {
        Group {
            if home.clientId == nil {
                ContentUnavailableView("No profile", systemImage: "person.crop.circle.badge.xmark")
            } else {
                listBody
            }
        }
        .navigationTitle("Updates")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Mark all read") {
                    Task {
                        guard let cid = home.clientId else { return }
                        await model.markAllRead(clientId: cid, inbox: inbox)
                        await home.reloadNotificationsCount()
                    }
                }
                .tint(Club360Theme.tealDark)
            }
        }
        .task(id: home.clientId) {
            guard let cid = home.clientId else { return }
            await model.load(clientId: cid)
        }
        .refreshable {
            guard let cid = home.clientId else { return }
            await model.load(clientId: cid, showLoading: false)
        }
    }

    private var listBody: some View {
        ZStack {
            Club360ScreenBackground()

            Group {
                if model.isLoading {
                    ProgressView()
                        .tint(Club360Theme.tealDark)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if model.items.isEmpty {
                    Text("No updates yet.")
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(model.items, id: \.id) { n in
                            notificationCard(n)
                                .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)
                                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                    Button(role: .destructive) {
                                        Task { await deleteNotification(n) }
                                    } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
                                }
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
        }
    }

    private func deleteNotification(_ n: ClientNotificationDTO) async {
        guard let id = n.rowId, let cid = home.clientId else { return }
        await model.deleteNotification(notificationId: id, clientId: cid)
        await home.reloadNotificationsCount()
    }

    private func notificationCard(_ n: ClientNotificationDTO) -> some View {
        let showUnreadDot: Bool = {
            switch inbox {
            case .member: return n.readAt == nil
            case .coach: return n.coachReadAt == nil
            }
        }()

        return Button {
            Task {
                // Member shell: `openNotification` resets `homePath`, which pops this screen — do not call `dismiss()` too (double-pop / broken toolbar hits).
                // Coach client hub: no tab router — pop with `dismiss()` only.
                if tabRouter != nil {
                    tabRouter?.openNotification(n)
                }
                if let id = n.rowId, let cid = home.clientId {
                    await model.markOneRead(notificationId: id, clientId: cid, inbox: inbox)
                }
                await home.reloadNotificationsCount()
                if tabRouter == nil {
                    dismiss()
                }
            }
        } label: {
            HStack(alignment: .top, spacing: 12) {
                if showUnreadDot {
                    Circle()
                        .fill(Club360Theme.peachDeep)
                        .frame(width: 10, height: 10)
                        .padding(.top, 4)
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(n.title)
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(Club360Theme.cardTitle)
                    Text(n.body)
                        .font(.body)
                        .foregroundStyle(Club360Theme.cardTitle)
                    if let created = n.createdAt {
                        Text(Club360Formatting.formatPaymentInstant(created))
                            .font(.caption)
                            .foregroundStyle(Club360Theme.captionOnGlass)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
            .club360Glass()
        }
        .buttonStyle(.plain)
    }
}

@Observable
@MainActor
private final class MyNotificationsViewModel {
    var isLoading = true
    var items: [ClientNotificationDTO] = []

    /// - Parameter showLoading: When false, avoids swapping `List` for `ProgressView` during swipe / mark-read (prevents UICollectionView update crashes).
    func load(clientId: String, showLoading: Bool = true) async {
        if showLoading { isLoading = true }
        defer { if showLoading { isLoading = false } }
        items = (try? await ClientDataService.fetchClientNotifications(clientId: clientId)) ?? []
    }

    func markOneRead(notificationId: String, clientId: String, inbox: MyNotificationsInboxKind) async {
        switch inbox {
        case .member:
            try? await ClientDataService.markNotificationRead(notificationId: notificationId)
        case .coach:
            try? await ClientDataService.markNotificationReadAsCoach(notificationId: notificationId)
        }
        await load(clientId: clientId, showLoading: false)
    }

    func markAllRead(clientId: String, inbox: MyNotificationsInboxKind) async {
        switch inbox {
        case .member:
            try? await ClientDataService.markAllNotificationsRead(clientId: clientId)
        case .coach:
            try? await ClientDataService.markAllCoachNotificationsReadForClient(clientId: clientId)
        }
        await load(clientId: clientId, showLoading: false)
    }

    func deleteNotification(notificationId: String, clientId: String) async {
        try? await ClientDataService.deleteClientNotification(notificationId: notificationId)
        await load(clientId: clientId, showLoading: false)
    }
}
