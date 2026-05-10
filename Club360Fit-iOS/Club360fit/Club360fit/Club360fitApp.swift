//
//  Club360fitApp.swift
//  Club360fit
//

import Auth
import SwiftUI

@main
struct Club360fitApp: App {
    @State private var authSession = Club360AuthSession()
    @UIApplicationDelegateAdaptor(PushNotificationService.self) private var pushNotifications

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(authSession)
                .onOpenURL { url in
                    // Password-reset links: `club360fit://reset?...` — add URL scheme in Xcode → Target → Info.
                    Club360FitSupabase.handleAuthRedirectURL(url)
                }
                .onChange(of: authSession.session?.user.id.uuidString) { _, _ in
                    pushNotifications.registerCachedTokenIfPossible()
                }
        }
    }
}
