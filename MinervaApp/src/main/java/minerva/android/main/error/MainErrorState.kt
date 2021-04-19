package minerva.android.main.error

sealed class MainErrorState
object BaseError : MainErrorState()
class UpdateCredentialError(val throwable: Throwable = Throwable()) : MainErrorState()
object UpdatePendingTransactionError : MainErrorState()
object NotExistedIdentity : MainErrorState()
class RequestedFields(val identityName: String) : MainErrorState()