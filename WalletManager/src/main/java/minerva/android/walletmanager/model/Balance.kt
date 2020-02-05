package minerva.android.walletmanager.model

import java.math.BigDecimal

data class Balance(
    var cryptoBalance: BigDecimal = BigDecimal.ZERO,
    var fiatBalance: BigDecimal = BigDecimal.ZERO
)