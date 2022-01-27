package minerva.android.extension.validator

import minerva.android.extension.R
import java.math.BigDecimal

object Validator {

    private const val HEX_PREFIX = "0x"
    private const val DOT = "."
    private const val INVALID_INDEX = -1

    fun validateIsFilled(content: String?): ValidationResult =
        if (content.isNullOrBlank()) ValidationResult.error(R.string.field_cannot_be_empty)
        else ValidationResult(true)

    fun validateAmountField(amount: String?, balance: BigDecimal): ValidationResult =
        when {
            amount.isNullOrBlank() -> ValidationResult.error(R.string.field_cannot_be_empty)
            BigDecimal(amount) > balance -> ValidationResult.error(R.string.not_enough_funds_error)
            else -> ValidationResult(true)
        }

    fun validateAmountFieldWithDecimalCheck(amount: String, balance: BigDecimal, decimals: Int = 0): ValidationResult {
        val validateAmountFieldResult = validateAmountField(amount, balance)
        return when {
            validateAmountFieldResult.hasError -> validateAmountFieldResult
            decimals == 0 && amount.indexOf(DOT) != INVALID_INDEX -> ValidationResult.error(R.string.amount_must_not_contain_decimal_digits)
            decimals < amount.lastIndex - amount.indexOf(DOT) -> ValidationResult.error(R.string.too_many_digits_after_decimal)
            else -> ValidationResult(true)
        }
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
}