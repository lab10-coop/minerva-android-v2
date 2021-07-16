package minerva.android.walletmanager.model.minervaprimitives.account

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import java.math.BigDecimal

data class Account(
    val id: Int,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    override var address: String = String.Empty,
    override var name: String = String.Empty,
    var chainId: Int = Int.InvalidValue,
    override var isDeleted: Boolean = false,
    var cryptoBalance: BigDecimal = BigDecimal.ZERO,
    var accountTokens: List<AccountToken> = listOf(),
    var coinRate: Double = Double.InvalidValue,
    var fiatBalance: BigDecimal = Double.InvalidValue.toBigDecimal(),
    var owners: List<String>? = null,
    var contractAddress: String = String.Empty,
    var isPending: Boolean = false,
    var dappSessionCount: Int = 0,
    override val bindedOwner: String = String.Empty,
    override var isHide: Boolean = false,
    private val _isTestNetwork: Boolean = false,
    var isError: Boolean = false
) : MinervaPrimitive(address, name, isDeleted, bindedOwner) {

    val masterOwnerAddress: String
        get() = owners?.last().orEmpty()

    val network: Network
        get() = if (isEmptyAccount) Network(chainId = Int.InvalidValue) else NetworkManager.getNetwork(chainId)

    val isEmptyAccount: Boolean
        get() = chainId == Int.InvalidValue

    val shouldShow: Boolean
        get() = !isEmptyAccount && !isHide && !isDeleted

    val isTestNetwork: Boolean
        get() = if (network.chainId != Int.InvalidValue) network.testNet else _isTestNetwork

    fun getToken(tokenAddress: String): AccountToken =
        accountTokens.find { it.token.address == tokenAddress } ?: AccountToken(ERC20Token(Int.InvalidIndex))

    fun getTokenIndex(tokenAddress: String) = accountTokens.indexOf(getToken(tokenAddress))

}