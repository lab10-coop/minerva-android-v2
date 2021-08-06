package minerva.android.onboarding.restore.state

sealed class RestoreWalletState
object ValidMnemonic : RestoreWalletState()
object InvalidMnemonicWords : RestoreWalletState()
object InvalidMnemonicLength : RestoreWalletState()
object WalletConfigNotFound : RestoreWalletState()
object GenericServerError : RestoreWalletState()
class Loading(val isLoading: Boolean) : RestoreWalletState()
object WalletConfigCreated : RestoreWalletState()
