package minerva.android.services.login

import minerva.android.accounts.walletconnect.WalletConnectAlertType
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectProposalNamespace

// todo: why is this duplicate with WalletConnectState?

sealed class ServicesScannerViewState
object DefaultState : ServicesScannerViewState()
object CloseScannerState : ServicesScannerViewState()
data class Error(val error: Throwable) : ServicesScannerViewState()
data class ProgressBarState(val isVisible: Boolean) : ServicesScannerViewState()

//--> Login states
data class ServiceLoginResult(val qrCode: ServiceQrCode) : ServicesScannerViewState()
data class CredentialsLoginResult(val message: String) : ServicesScannerViewState()
data class UpdateCredentialsLoginResult(val qrCode: CredentialQrCode) : ServicesScannerViewState()

//--> WC states
object CorrectWalletConnectResult : ServicesScannerViewState()
data class WalletConnectSessionRequestResult(
    val meta: WalletConnectPeerMeta,
    val network: BaseNetworkData,
    val dialogType: WalletConnectAlertType
) : ServicesScannerViewState()
data class WalletConnectSessionRequestResultV2(
    val meta: WalletConnectPeerMeta,
    val numberOfNonEip155Chains: Int,
    val eip155ProposalNamespace: WalletConnectProposalNamespace
) : ServicesScannerViewState()

data class WalletConnectDisconnectResult(val sessionName: String = String.Empty) : ServicesScannerViewState()
data class WalletConnectConnectionError(val error: Throwable, val sessionName: String = String.Empty) : ServicesScannerViewState()
data class WalletConnectUpdateDataState(val network: BaseNetworkData, val dialogType: WalletConnectAlertType) :
    ServicesScannerViewState()