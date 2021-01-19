package minerva.android.walletConnect.model.jsonRpc

import minerva.android.walletConnect.client.JSONRPC_VERSION

data class JsonRpcResponse<T>(
    val jsonrpc: String = JSONRPC_VERSION,
    val id: Long,
    val result: T
)