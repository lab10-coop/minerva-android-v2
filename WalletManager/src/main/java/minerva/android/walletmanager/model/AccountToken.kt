package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.token.ERC20Token
import java.math.BigDecimal

data class AccountToken(
    val token: ERC20Token,
    var balance: BigDecimal = Int.InvalidValue.toBigDecimal()
) {
    override fun equals(other: Any?): Boolean =
        (other as? AccountToken)?.let {
            this.token.address.equals(it.token.address, true)
        }.orElse { false }
}