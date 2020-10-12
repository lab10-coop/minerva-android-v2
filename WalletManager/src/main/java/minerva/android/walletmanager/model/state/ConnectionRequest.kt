package minerva.android.walletmanager.model.state

sealed class ConnectionRequest<out T> {
    data class ServiceNotConnected<out T>(val data: T) : ConnectionRequest<T>()
    object VCNotFound : ConnectionRequest<Nothing>()
    data class ServiceConnected<out T>(val data: T) : ConnectionRequest<T>()
}
/*VC - Verifiable Credential*/