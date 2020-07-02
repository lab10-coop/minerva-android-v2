package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

data class Value(
    val index: Int,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    override var address: String = String.Empty,
    override var name: String = String.Empty,
    override val network: String = String.Empty,
    override var isDeleted: Boolean = false,
    var cryptoBalance: BigDecimal = Int.InvalidValue.toBigDecimal(),
    var assets: List<Asset> = listOf(),
    var fiatBalance: BigDecimal = BigDecimal.ZERO,
    var owners: List<String>? = null,
    var contractAddress: String = String.Empty,
    var pending: Boolean = false,
    override val bindedOwner: String = String.Empty
): Account(address, name, isDeleted, bindedOwner, network) {
    constructor(value: Value, isDeleted: Boolean) : this(
        value.index,
        value.publicKey,
        value.privateKey,
        value.address,
        value.name,
        value.network,
        isDeleted,
        owners = value.owners,
        pending = false
    )

    val masterOwnerAddress: String
        get() = owners?.last().orEmpty()
}