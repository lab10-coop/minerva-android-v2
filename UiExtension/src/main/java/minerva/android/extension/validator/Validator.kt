package minerva.android.extension.validator

import minerva.android.extension.R

object Validator {
    fun validateIsFilled(content: String?): ValidationResult =
        if (content.isNullOrBlank()) ValidationResult.error(R.string.field_cannot_be_empty)
        else ValidationResult(true)
}