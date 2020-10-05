package minerva.android.walletmanager.model.state

sealed class VCRequestState<out T> {
    data class Found<out T>(val data: T) : VCRequestState<T>()
    object NotFound : VCRequestState<Nothing>()
}