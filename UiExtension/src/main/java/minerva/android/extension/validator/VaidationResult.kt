package minerva.android.extension.validator

import androidx.annotation.StringRes

data class ValidationResult(
    val isSuccessful: Boolean = false,
    @StringRes val errorMessageId: Int = 0
) {
    val hasError = !isSuccessful

    companion object {
        fun error(@StringRes messageId: Int) =
            ValidationResult(false, messageId)
    }
}