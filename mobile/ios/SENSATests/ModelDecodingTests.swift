import XCTest
@testable import SENSA

final class ModelDecodingTests: XCTestCase {
    func testAuthResponseDecoding() throws {
        let json = #"""
        {
          "token": "jwt-token",
          "userId": "f8ef34af-73e9-4a96-93ce-2b4558709c2c",
          "email": "test@sensanotify.com"
        }
        """#

        let response = try JSONDecoder().decode(AuthResponse.self, from: Data(json.utf8))
        XCTAssertEqual(response.token, "jwt-token")
        XCTAssertEqual(response.email, "test@sensanotify.com")
        XCTAssertEqual(response.userId.uuidString.lowercased(), "f8ef34af-73e9-4a96-93ce-2b4558709c2c")
    }

    func testEmergencyDecoding() throws {
        let json = #"""
        {
          "id": 42,
          "user_id": "f8ef34af-73e9-4a96-93ce-2b4558709c2c",
          "title": "Fire in the building",
          "description": "Smoke on the second floor",
          "files": [
            { "fileId": 10, "url": "https://example.com/a.jpg" }
          ],
          "country": "Poland",
          "city": "Opole",
          "street": "Mikołajczyka",
          "alarm_timestamp": "2026-11-02T10:00:00Z",
          "status": "REPORTED",
          "created_at": "2026-11-02T10:01:00Z"
        }
        """#

        let emergency = try JSONDecoder().decode(Emergency.self, from: Data(json.utf8))
        XCTAssertEqual(emergency.id, 42)
        XCTAssertEqual(emergency.statusKind, .reported)
        XCTAssertEqual(emergency.files?.first?.fileId, 10)
        XCTAssertEqual(emergency.city, "Opole")
    }

    func testFileUploadResultDecoding() throws {
        let rejectedJSON = #"""
        {
          "fileId": 7,
          "url": null,
          "moderationStatus": "REJECTED",
          "confidencePercentage": 92.5
        }
        """#

        let rejected = try JSONDecoder().decode(FileUploadResult.self, from: Data(rejectedJSON.utf8))
        XCTAssertEqual(rejected.fileId, 7)
        XCTAssertNil(rejected.url)
        XCTAssertFalse(rejected.isApproved)

        let approvedJSON = #"""
        {
          "fileId": 8,
          "url": "https://example.com/b.jpg",
          "moderationStatus": "APPROVED",
          "confidencePercentage": 1.2
        }
        """#

        let approved = try JSONDecoder().decode(FileUploadResult.self, from: Data(approvedJSON.utf8))
        XCTAssertTrue(approved.isApproved)
        XCTAssertEqual(approved.url, "https://example.com/b.jpg")
    }
}
