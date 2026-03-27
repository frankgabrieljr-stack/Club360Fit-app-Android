import Foundation

/// `workout_session_logs` — mirrors Android `WorkoutSessionLogDto`.
struct WorkoutSessionLogDTO: Decodable, Sendable {
    let id: String?
    let clientId: String
    let sessionDate: String
    let weekStart: String
    let noteToCoach: String?
    let coachReply: String?
    let coachRepliedAt: String?

    enum CodingKeys: String, CodingKey {
        case id
        case clientId = "client_id"
        case sessionDate = "session_date"
        case weekStart = "week_start"
        case noteToCoach = "note_to_coach"
        case coachReply = "coach_reply"
        case coachRepliedAt = "coach_replied_at"
    }
}

struct WorkoutSessionLogInsert: Encodable, Sendable {
    let clientId: String
    let sessionDate: String
    let weekStart: String
    let noteToCoach: String?

    enum CodingKeys: String, CodingKey {
        case clientId = "client_id"
        case sessionDate = "session_date"
        case weekStart = "week_start"
        case noteToCoach = "note_to_coach"
    }
}
