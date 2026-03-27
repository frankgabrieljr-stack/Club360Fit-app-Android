import Foundation
import Supabase

/// Calls Edge Function `set-user-role` (deploy from repo `supabase/functions/set-user-role`).
private struct SetUserRolePayload: Encodable {
    let target_user_id: String
    let role: String
}

private struct SetUserRoleResponse: Decodable {
    let ok: Bool?
    let error: String?
}

enum AdminRoleService {
    /// Sets Auth `user_metadata.role` to `"admin"` or `"client"`. Requires a deployed `set-user-role` function and service role on the server.
    static func setUserRole(targetAuthUserId: String, role: String) async throws {
        let trimmed = targetAuthUserId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            throw NSError(domain: "Club360Fit", code: 400, userInfo: [NSLocalizedDescriptionKey: "Missing member account."])
        }
        guard role == "admin" || role == "client" else {
            throw NSError(domain: "Club360Fit", code: 400, userInfo: [NSLocalizedDescriptionKey: "Invalid role."])
        }
        let payload = SetUserRolePayload(target_user_id: trimmed.lowercased(), role: role)
        let options = try await Club360FitSupabase.functionInvokeOptions(body: payload)
        let response: SetUserRoleResponse = try await Club360FitSupabase.shared.functions.invoke(
            "set-user-role",
            options: options
        )
        if response.ok != true {
            let msg = response.error ?? "Could not update role. Deploy the set-user-role Edge Function in Supabase."
            throw NSError(domain: "Club360Fit", code: 1, userInfo: [NSLocalizedDescriptionKey: msg])
        }
    }
}
