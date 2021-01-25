package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

data class Account(
    val id: Int,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    override var address: String = String.Empty,
    override var name: String = String.Empty,
    override val networkShort: String = String.Empty,
    override var isDeleted: Boolean = false,
    var cryptoBalance: BigDecimal = BigDecimal.ZERO,
    var accountTokens: List<AccountToken> = listOf(),
    var fiatBalance: BigDecimal = Int.InvalidValue.toBigDecimal(),
    var owners: List<String>? = null,
    var contractAddress: String = String.Empty,
    var isPending: Boolean = false,
    var dappSessionCount: Int = 0,
    override val bindedOwner: String = String.Empty
) : MinervaPrimitive(address, name, isDeleted, bindedOwner, networkShort) {
    constructor(account: Account, isDeleted: Boolean) : this(
        account.id,
        account.publicKey,
        account.privateKey,
        account.address,
        String.Empty,
        account.networkShort,
        isDeleted,
        owners = account.owners,
        isPending = false,
        dappSessionCount = 0
    )

    val masterOwnerAddress: String
        get() = owners?.last().orEmpty()
}