import Observation
import SwiftUI

/// All `client_notifications` across coached members (RLS). Shown from the main Coach Hub bell.
struct CoachHubNotificationsView: View {
    let clientNameById: [String: String]
    var onUnreadChanged: () -> Void = {}

    @State private var model = CoachHubNotificationsModel()

    var body: some View {
        Group {
            if model.isLoading, model.items.isEmpty {
                ZStack {
                    Club360ScreenBackground()
                    ProgressView()
                        .tint(Club360Theme.tealDark)
                }
            } else if model.items.isEmpty {
                ZStack {
                    Club360ScreenBackground()
                    ContentUnavailableView(
                        "No updates yet",
                        systemImage: "bell",
                        description: Text("Alerts from your clients and system messages will show here.")
                    )
                }
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
                        try? await ClientDataService.markAllNotificationsReadForCoach()
                        await model.load(showLoading: false)
                        onUnreadChanged()
                    }
                }
                .tint(Club360Theme.tealDark)
            }
        }
        .task {
            await model.load()
        }
        .refreshable {
            await model.load(showLoading: false)
        }
    }

    private var listBody: some View {
        ZStack {
            Club360ScreenBackground()

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

    private func deleteNotification(_ n: ClientNotificationDTO) async {
        guard let id = n.rowId else { return }
        try? await ClientDataService.deleteClientNotification(notificationId: id)
        await model.load(showLoading: false)
        onUnreadChanged()
    }

    private func notificationCard(_ n: ClientNotificationDTO) -> some View {
        Button {
            Task {
                if let id = n.rowId {
                    try? await ClientDataService.markNotificationReadAsCoach(notificationId: id)
                    await model.load(showLoading: false)
                    onUnreadChanged()
                }
            }
        } label: {
            HStack(alignment: .top, spacing: 12) {
                if n.coachReadAt == nil {
                    Circle()
                        .fill(Club360Theme.peachDeep)
                        .frame(width: 10, height: 10)
                        .padding(.top, 4)
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(clientNameById[n.clientId] ?? "Member")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Club360Theme.tealDark)
                    Text(n.title)
                        .font(.headline.weight(.semibold))
                        .foregroundStyle(Club360Theme.cardTitle)
                    Text(n.body)
                        .font(.body)
                        .foregroundStyle(Club360Theme.cardTitle)
                    if let created = n.createdAt {
                        Text(Club360Formatting.formatPaymentInstant(created))
                            .font(.caption)
                            .foregroundStyle(Club360Theme.cardSubtitle)
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
private final class CoachHubNotificationsModel {
    var isLoading = true
    var items: [ClientNotificationDTO] = []

    func load(showLoading: Bool = true) async {
        if showLoading { isLoading = true }
        defer { if showLoading { isLoading = false } }
        items = (try? await ClientDataService.fetchNotificationsForCoach(limit: 80)) ?? []
    }
}
