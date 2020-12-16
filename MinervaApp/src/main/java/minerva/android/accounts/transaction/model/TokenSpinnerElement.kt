package minerva.android.accounts.transaction.model

import minerva.android.R
import minerva.android.kotlinUtils.Empty

data class TokenSpinnerElement(
    val name: String = String.Empty,
    val logo: Int = R.drawable.ic_default_token
)