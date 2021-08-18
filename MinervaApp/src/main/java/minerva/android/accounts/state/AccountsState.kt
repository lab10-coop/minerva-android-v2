package minerva.android.accounts.state

sealed class AccountsErrorState
object BaseError : AccountsErrorState()
object BalanceIsNotEmptyAndHasMoreOwnersError : AccountsErrorState()
object BalanceIsNotEmptyError : AccountsErrorState()
object IsNotSafeAccountMasterOwnerError : AccountsErrorState()
class AutomaticBackupError(val throwable: Throwable) : AccountsErrorState()
object RefreshBalanceError : AccountsErrorState()
object NoFunds : AccountsErrorState()

interface BalanceState
sealed class TokenBalanceState : BalanceState
sealed class CoinBalanceState : BalanceState
object UpdateAllState : BalanceState

class CoinBalanceUpdate(val index: Int) : CoinBalanceState()
object CoinBalanceCompleted : CoinBalanceState()

class TokenBalanceUpdate(val index: Int) : TokenBalanceState()
object TokenBalanceCompleted : TokenBalanceState()