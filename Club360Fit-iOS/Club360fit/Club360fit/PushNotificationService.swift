import Foundation
import Supabase
import SwiftUI
import UIKit
import UserNotifications

private struct DeviceTokenRegistrationPayload: Encodable {
    let platform: String
    let token: String
    let environment: String
    let app_version: String
    let device_id: String?
}

private struct DeviceTokenRegistrationResponse: Decodable {
    let ok: Bool?
    let error: String?
}

final class PushNotificationService: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private var apnsToken: String?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        requestAuthorization()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        apnsToken = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        registerCachedTokenIfPossible()
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Push registration failed: \(error.localizedDescription)")
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            guard granted else { return }
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }

    func registerCachedTokenIfPossible() {
        guard let token = apnsToken, !token.isEmpty else { return }
        Task {
            do {
                let payload = DeviceTokenRegistrationPayload(
                    platform: "ios_apns",
                    token: token,
                    environment: Self.apnsEnvironment,
                    app_version: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
                    device_id: UIDevice.current.identifierForVendor?.uuidString
                )
                let options = try await Club360FitSupabase.functionInvokeOptions(body: payload)
                let response: DeviceTokenRegistrationResponse = try await Club360FitSupabase.shared.functions.invoke(
                    "register-device-token",
                    options: options
                )
                if response.ok != true, let error = response.error {
                    print("Push token registration failed: \(error)")
                }
            } catch {
                // No signed-in session yet, missing function deploy, or offline. Next auth/session change retries.
                print("Push token registration skipped: \(error.localizedDescription)")
            }
        }
    }

    private static var apnsEnvironment: String {
        #if DEBUG
        "sandbox"
        #else
        "production"
        #endif
    }
}
