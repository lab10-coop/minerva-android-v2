package minerva.android.walletConnect.model.jsonRpc

data class JsonRpcError(
    val code: Int,
    val message: String
) {
    companion object {
        fun serverError(message: String) = JsonRpcError(SERVER_ERROR, message)
        fun invalidParams(message: String) = JsonRpcError(INVALID_PARAMS, message)
        fun invalidRequest(message: String) = JsonRpcError(INVALID_REQUEST, message)
        fun parseError(message: String) = JsonRpcError(PARSE_ERROR, message)
        fun methodNotFound(message: String) = JsonRpcError(METHOD_NOT_FOUND, message)

        private const val SERVER_ERROR = -32000
        private const val INVALID_PARAMS = -32602
        private const val INVALID_REQUEST = -32600
        private const val PARSE_ERROR = -32700
        private const val METHOD_NOT_FOUND = -32601
    }
}