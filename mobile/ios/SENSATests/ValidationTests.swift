import XCTest
@testable import SENSA

@MainActor
final class ValidationTests: XCTestCase {
    func testValidEmails() {
        XCTAssertTrue(AuthViewModel.isValidEmail("test@sensanotify.com"))
        XCTAssertTrue(AuthViewModel.isValidEmail("user.name+tag@example.co.uk"))
    }

    func testInvalidEmails() {
        XCTAssertFalse(AuthViewModel.isValidEmail(""))
        XCTAssertFalse(AuthViewModel.isValidEmail("plainaddress"))
        XCTAssertFalse(AuthViewModel.isValidEmail("user@"))
        XCTAssertFalse(AuthViewModel.isValidEmail("user@example"))
    }

    func testISO8601Parsing() {
        XCTAssertNotNil(DateText.parse("2026-11-02T10:00:00Z"))
        XCTAssertNotNil(DateText.parse("2026-11-02T10:00:00.123Z"))
        XCTAssertNil(DateText.parse("not-a-date"))
    }
}
