package minerva.android.extension.validator

import minerva.android.extension.R
import java.math.BigDecimal

object Validator {

    private const val HEX_PREFIX = "0x"
    private const val DOT = "."
    private const val ZERO = "0"

    fun validateIsFilled(content: String?): ValidationResult =
        if (content.isNullOrBlank()) ValidationResult.error(R.string.field_cannot_be_empty)
        else ValidationResult(true)

    fun validateAmountField(amount: String?, balance: BigDecimal): ValidationResult {
        return when {
            amount.isNullOrBlank() -> ValidationResult.error(R.string.field_cannot_be_empty)
            BigDecimal(amount) > balance -> ValidationResult.error(R.string.not_enough_funds_error)
            amount == ZERO -> ValidationResult.error(R.string.amount_cannot_be_zero_error)
            else -> ValidationResult(true)
        }
    }

    fun validateAddress(address: String, isAddressValid: Boolean): ValidationResult {
        return when {
            address.isEmpty() -> ValidationResult.error(minerva.android.extension.R.string.field_cannot_be_empty)
            isEnsName(address) -> ValidationResult(true)
            isAddressValid -> ValidationResult(true)
            !isAddressValid -> ValidationResult.error(R.string.invalid_account_address)
            else -> ValidationResult.error(R.string.invalid_account_address)
        }
    }

    fun isEnsName(name: String) = !name.startsWith(HEX_PREFIX) && name.contains(DOT)
}