package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class Payment(
    val amount: String? = String.Empty,
    val iban: String? = String.Empty,
    val recipient: String? = String.Empty,
    val serviceName: String? = String.Empty,
    val shortName: String = String.Empty,
    val url: String? = String.Empty
)