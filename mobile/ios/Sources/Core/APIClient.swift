import Foundation

struct UploadFile {
    let data: Data
    let filename: String
    let mimeType: String
}

final class APIClient {
    var token: String?
    var onUnauthorized: (() -> Void)?

    private let baseURL: URL
    private let urlSession: URLSession
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    init(baseURL: URL) {
        self.baseURL = baseURL

        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 60
        configuration.timeoutIntervalForResource = 300
        self.urlSession = URLSession(configuration: configuration)
    }

    // MARK: - Auth

    func login(email: String, password: String) async throws -> AuthResponse {
        let body = try encoder.encode(LoginRequest(email: email, password: password))
        return try await send(makeRequest("/v1/auth/login", method: "POST", body: body, contentType: "application/json"))
    }

    func register(_ payload: RegisterRequest) async throws -> AuthResponse {
        let body = try encoder.encode(payload)
        return try await send(makeRequest("/v1/auth/register", method: "POST", body: body, contentType: "application/json"))
    }

    // MARK: - User profile

    func getUser(userId: UUID) async throws -> UserProfile {
        try await send(makeRequest("/v1/users/\(userId.uuidString)"))
    }

    func updateUser(userId: UUID, payload: UserUpdateRequest) async throws -> UserProfile {
        let body = try encoder.encode(payload)
        return try await send(makeRequest("/v1/users/\(userId.uuidString)", method: "PATCH", body: body, contentType: "application/json"))
    }

    // MARK: - Files

    func uploadFiles(_ files: [UploadFile]) async throws -> [FileUploadResult] {
        guard !files.isEmpty else { return [] }

        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()

        for file in files {
            body.appendUTF8("--\(boundary)\r\n")
            body.appendUTF8("Content-Disposition: form-data; name=\"files\"; filename=\"\(file.filename)\"\r\n")
            body.appendUTF8("Content-Type: \(file.mimeType)\r\n\r\n")
            body.append(file.data)
            body.appendUTF8("\r\n")
        }
        body.appendUTF8("--\(boundary)--\r\n")

        let request = makeRequest(
            "/v1/files",
            method: "POST",
            body: body,
            contentType: "multipart/form-data; boundary=\(boundary)"
        )
        return try await send(request)
    }

    func linkFiles(_ payload: FileLinkRequest) async throws -> [FileResponse] {
        let body = try encoder.encode(payload)
        return try await send(makeRequest("/v1/files/link", method: "PATCH", body: body, contentType: "application/json"))
    }

    // MARK: - Emergencies

    func createEmergency(_ payload: CreateEmergencyRequest) async throws -> Emergency {
        let body = try encoder.encode(payload)
        return try await send(makeRequest("/v1/emergencies", method: "POST", body: body, contentType: "application/json"))
    }

    func getMyEmergencies() async throws -> [Emergency] {
        try await send(makeRequest("/v1/emergencies/my"))
    }

    func getEmergency(id: Int64) async throws -> Emergency {
        try await send(makeRequest("/v1/emergencies/\(id)"))
    }

    // MARK: - Internals

    private func url(_ path: String) -> URL {
        URL(string: baseURL.absoluteString + path)!
    }

    private func makeRequest(
        _ path: String,
        method: String = "GET",
        body: Data? = nil,
        contentType: String? = nil
    ) -> URLRequest {
        var request = URLRequest(url: url(path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if let token, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            request.httpBody = body
        }
        if let contentType {
            request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        }
        return request
    }

    private func send<T: Decodable>(_ request: URLRequest) async throws -> T {
        do {
            let (data, response) = try await urlSession.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }

            if http.statusCode == 401 {
                onUnauthorized?()
                throw APIError.unauthorized
            }

            guard (200..<300).contains(http.statusCode) else {
                throw APIError.http(status: http.statusCode, message: errorMessage(from: data))
            }

            do {
                return try decoder.decode(T.self, from: data)
            } catch {
                throw APIError.decoding(error)
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.transport(error, url: request.url?.absoluteString ?? "unknown")
        }
    }

    private func errorMessage(from data: Data) -> String {
        if let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            if let message = object["message"] as? String, !message.isEmpty {
                return message
            }
            if let error = object["error"] as? String, !error.isEmpty {
                return error
            }
        }
        if let text = String(data: data, encoding: .utf8), !text.isEmpty {
            return String(text.prefix(300))
        }
        return "Request failed."
    }
}

private extension Data {
    mutating func appendUTF8(_ string: String) {
        append(contentsOf: string.utf8)
    }
}
