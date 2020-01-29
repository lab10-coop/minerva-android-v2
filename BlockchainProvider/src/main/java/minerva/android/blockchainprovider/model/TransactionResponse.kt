package minerva.android.blockchainprovider.model

import minerva.android.kotlinUtils.Empty

data class TransactionResponse(
    val isSuccess: Boolean,
    val errorMessage: String = String.Empty
)