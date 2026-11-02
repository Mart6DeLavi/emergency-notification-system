import Foundation

enum DateText {
    static func display(_ iso: String?) -> String {
        guard let iso, let date = parse(iso) else { return "—" }
        return date.formatted(date: .abbreviated, time: .shortened)
    }

    static func parse(_ iso: String) -> Date? {
        let fractional = ISO8601DateFormatter()
        fractional.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = fractional.date(from: iso) {
            return date
        }

        let plain = ISO8601DateFormatter()
        plain.formatOptions = [.withInternetDateTime]
        return plain.date(from: iso)
    }

    static func nowISO8601() -> String {
        ISO8601DateFormatter().string(from: Date())
    }
}
