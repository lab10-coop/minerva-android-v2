package minerva.android.extension.validator

import minerva.android.extension.R
import minerva.android.extension.isNumber
import java.math.BigDecimal

object Validator {

    private const val HEX_PREFIX = "0x"
    private const val DOT = "."

    fun validateIsFilled(content: String?): ValidationResult =
        if (content.isNullOrBlank()) ValidationResult.error(R.string.field_cannot_be_empty)
        else ValidationResult(true)

    fun validateAmountField(amount: String?, balance: BigDecimal): ValidationResult =
        when {
            amount.isNullOrBlank() -> ValidationResult.error(R.string.field_cannot_be_empty)
            BigDecimal(amount) > balance -> ValidationResult.error(R.string.not_enough_funds_error)
            else -> ValidationResult(true)
        }

    fun validateAddress(address: String, isAddressValid: Boolean, validationErrorMessage: Int): ValidationResult {
        return when {
            address.isEmpty() -> ValidationResult.error(R.string.field_cannot_be_empty)
            isEnsName(address) -> ValidationResult(true)
            isAddressValid -> ValidationResult(true)
            !isAddressValid -> ValidationResult.error(validationErrorMessage)
            else -> ValidationResult.error(validationErrorMessage)
        }
    }

    fun isEnsName(name: String) = !name.startsWith(HEX_PREFIX) && name.contains(DOT)

    fun validateTokenId(tokenId: String?): ValidationResult =
        when {
            tokenId.isNullOrBlank() -> ValidationResult.error(R.string.field_cannot_be_empty)
            !tokenId.isNumber() -> ValidationResult.error(R.string.field_must_be_a_number)
            else -> ValidationResult(true)
        }
}