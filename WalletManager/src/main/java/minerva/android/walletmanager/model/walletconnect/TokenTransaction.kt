package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal

data class TokenTransaction(
    var allowance: BigDecimal? = null,
    var tokenSymbol: String = String.Empty,
    var tokenValue: String = String.Empty,
    var from: String = String.Empty,
    var to: String = String.Empty
)
