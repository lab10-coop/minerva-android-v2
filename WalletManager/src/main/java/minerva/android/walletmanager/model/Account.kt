package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

data class Account(
    val index: Int,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    override var address: String = String.Empty,
    override var name: String = String.Empty,
    override val network: String = String.Empty,
    override var isDeleted: Boolean = false,
    var cryptoBalance: BigDecimal = Int.InvalidValue.toBigDecimal(),
    var accountAssets: List<AccountAsset> = listOf(),
    var fiatBalance: BigDecimal = BigDecimal.ZERO,
    var owners: List<String>? = null,
    var contractAddress: String = String.Empty,
    var pending: Boolean = false,
    override val bindedOwner: String = String.Empty
): MinervaPrimitive(address, name, isDeleted, bindedOwner, network) {
    constructor(account: Account, isDeleted: Boolean) : this(
        account.index,
        account.publicKey,
        account.privateKey,
        account.address,
        account.name,
        account.network,
        isDeleted,
        owners = account.owners,
        pending = false
    )

    val masterOwnerAddress: String
        get() = owners?.last().orEmpty()
}