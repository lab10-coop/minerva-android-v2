package minerva.android.walletConnect.model.exceptions

class InvalidHmacException : Exception("Invalid HMAC")
class InvalidJsonRpcParamsException(val requestId: Long) : Exception("Invalid JSON RPC Request")
class InvalidSessionException : Exception("Invalid session")
class InvalidPayloadException : Exception("Invalid WCEncryptionPayload")