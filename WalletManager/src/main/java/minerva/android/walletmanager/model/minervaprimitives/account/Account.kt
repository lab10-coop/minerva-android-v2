package minerva.android.walletmanager.model.minervaprimitives.account

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.token.AccountToken
import java.math.BigDecimal

data class Account(
    val id: Int,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    override var address: String = String.Empty,
    override var name: String = String.Empty,
    val chainId: Int = Int.InvalidValue,
    override var isDeleted: Boolean = false,
    var cryptoBalance: BigDecimal = BigDecimal.ZERO,
    var accountTokens: List<AccountToken> = listOf(),
    var fiatBalance: BigDecimal = Int.InvalidValue.toBigDecimal(),
    var owners: List<String>? = null,
    var contractAddress: String = String.Empty,
    var isPending: Boolean = false,
    var dappSessionCount: Int = 0,
    override val bindedOwner: String = String.Empty
) : MinervaPrimitive(address, name, isDeleted, bindedOwner) {
    constructor(account: Account, isDeleted: Boolean) : this(
        account.id,
        account.publicKey,
        account.privateKey,
        account.address,
        String.Empty,
        account.chainId,
        isDeleted,
        owners = account.owners,
        isPending = false,
        dappSessionCount = 0
    )

    val masterOwnerAddress: String
        get() = owners?.last().orEmpty()

    val network: Network
        get() = NetworkManager.getNetwork(chainId)
}