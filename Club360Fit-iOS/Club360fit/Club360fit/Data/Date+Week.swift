import Foundation

extension Calendar {
    /// Sunday-start week containing the given date.
    static func weekStartSunday(containing date: Date) -> Date {
        let cal = Calendar.current
        let day = cal.startOfDay(for: date)
        let daysSinceSunday = cal.component(.weekday, from: day) - 1
        return cal.date(byAdding: .day, value: -daysSinceSunday, to: day) ?? day
    }
}

enum Club360DateFormats {
    static let postgresDay: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = .current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    static func dayString(_ date: Date) -> String {
        postgresDay.string(from: date)
    }

    /// User-facing calendar date (`MMM dd yyyy`), e.g. `Mar 24 2026`.
    static func displayDay(from date: Date) -> String {
        displayDay(fromPostgresDay: dayString(date))
    }

    /// Matches Android `LocalDate.toDisplayDate()` (`MMM dd yyyy`).
    static func displayDay(fromPostgresDay s: String) -> String {
        guard let d = postgresDay.date(from: s) else { return s }
        let f = DateFormatter()
        f.locale = .current
        f.timeZone = .current
        f.dateFormat = "MMM dd yyyy"
        return f.string(from: d)
    }
}
