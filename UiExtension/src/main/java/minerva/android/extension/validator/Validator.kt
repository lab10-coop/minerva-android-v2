package minerva.android.extension.validator

import minerva.android.extension.R
import java.math.BigDecimal

object Validator {

    private const val HEX_PREFIX = "0x"

    fun validateIsFilled(content: String?): ValidationResult =
        if (content.isNullOrBlank()) ValidationResult.error(R.string.field_cannot_be_empty)
        else ValidationResult(true)

    fun validateAmountField(amount: String?, balance: BigDecimal): ValidationResult {
        return when {
            amount.isNullOrBlank() -> ValidationResult.error(R.string.field_cannot_be_empty)
            BigDecimal(amount) >= balance -> ValidationResult.error(R.string.not_enough_funds_error)
            else -> ValidationResult(true)
        }
    }

    fun validateReceiverAddress(address: String?): ValidationResult {
        return when {
            address.isNullOrBlank() -> ValidationResult.error(R.string.field_cannot_be_empty)
            !address.startsWith(HEX_PREFIX) -> ValidationResult.error(R.string.invalid_account_address)
            else -> ValidationResult(true)
        }
    }
}