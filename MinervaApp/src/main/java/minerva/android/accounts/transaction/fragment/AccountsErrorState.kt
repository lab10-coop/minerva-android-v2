package minerva.android.accounts.transaction.fragment

sealed class AccountsErrorState
object BaseError : AccountsErrorState()
object BalanceIsNotEmptyAndHasMoreOwnersError : AccountsErrorState()
object BalanceIsNotEmptyError : AccountsErrorState()
object IsNotSafeAccountMasterOwnerError : AccountsErrorState()
class AutomaticBackupError(val throwable: Throwable) : AccountsErrorState()
object RefreshCoinBalancesError : AccountsErrorState()
object RefreshTokenBalancesError : AccountsErrorState()
object NoFunds : AccountsErrorState()
