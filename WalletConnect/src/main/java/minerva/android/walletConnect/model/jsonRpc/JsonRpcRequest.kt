package minerva.android.walletConnect.model.jsonRpc

import minerva.android.walletConnect.client.JSONRPC_VERSION
import minerva.android.walletConnect.model.enums.WCMethod

data class JsonRpcRequest<T>(
    val id: Long,
    val jsonrpc: String = JSONRPC_VERSION,
    val method: WCMethod?,
    val params: T
)