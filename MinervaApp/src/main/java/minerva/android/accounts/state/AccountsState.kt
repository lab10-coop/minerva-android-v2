package minerva.android.accounts.state

sealed class AccountsErrorState
object BaseError : AccountsErrorState()
object BalanceIsNotEmptyAndHasMoreOwnersError : AccountsErrorState()
object BalanceIsNotEmptyError : AccountsErrorState()
object IsNotSafeAccountMasterOwnerError : AccountsErrorState()
class AutomaticBackupError(val throwable: Throwable) : AccountsErrorState()
object RefreshCoinBalancesError : AccountsErrorState()
object RefreshTokenBalancesError : AccountsErrorState()
object NoFunds : AccountsErrorState()

sealed class CoinBalanceState
class CoinBalanceUpdate(val index: Int) : CoinBalanceState()
object CoinBalanceCompleted : CoinBalanceState()