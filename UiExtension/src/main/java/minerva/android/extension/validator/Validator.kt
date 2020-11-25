package minerva.android.extension.validator

import minerva.android.extension.R
import java.math.BigDecimal

object Validator {

    const val HEX_PREFIX = "0x"
    private const val DOT = "."

    fun validateIsFilled(content: String?): ValidationResult =
        if (content.isNullOrBlank()) ValidationResult.error(R.string.field_cannot_be_empty)
        else ValidationResult(true)

    fun validateAmountField(amount: String?, balance: BigDecimal): ValidationResult {
        return when {
            amount.isNullOrBlank() -> ValidationResult.error(R.string.field_cannot_be_empty)
            BigDecimal(amount) > balance -> ValidationResult.error(R.string.not_enough_funds_error)
            else -> ValidationResult(true)
        }
    }

    fun validateAddress(address: String, isAddressValid: Boolean): ValidationResult {
        return when {
            address.isEmpty() -> ValidationResult.error(minerva.android.extension.R.string.field_cannot_be_empty)
            !address.startsWith(HEX_PREFIX) && address.contains(DOT) -> ValidationResult(true)
            isAddressValid -> ValidationResult(true)
            !isAddressValid -> ValidationResult.error(R.string.invalid_account_address)
            else -> ValidationResult.error(R.string.invalid_account_address)
        }
    }
}