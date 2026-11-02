import Foundation

enum APIError: LocalizedError {
    case invalidResponse
    case unauthorized
    case http(status: Int, message: String)
    case decoding(Error)
    case transport(Error, url: String)

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Unexpected server response."
        case .unauthorized:
            return "Your session has expired. Please sign in again."
        case .http(_, let message):
            return message
        case .decoding:
            return "The server returned data in an unexpected format."
        case .transport(let error, let url):
            return "Network error (\(url)): \(error.localizedDescription)"
        }
    }
}
