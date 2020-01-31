package minerva.android.validator

import minerva.android.R
import minerva.android.extension.validator.ValidationResult
import minerva.android.extension.validator.Validator
import org.amshove.kluent.shouldEqualTo
import org.junit.Test
import java.math.BigDecimal

class FormValidatorTest {

    @Test
    fun `test email amount validator`() {
        checkExpectedEmptyResult(Validator.validateAmountField("", BigDecimal(9)))
        val incorrectEmailValidationResult = Validator.validateAmountField("5", BigDecimal(2))
        incorrectEmailValidationResult.hasError shouldEqualTo true
        incorrectEmailValidationResult.errorMessageId shouldEqualTo R.string.not_enough_funds_error
        val correctEmailValidationResult = Validator.validateAmountField("2", BigDecimal(5))
        correctEmailValidationResult.hasError shouldEqualTo false
    }


    @Test
    fun `test receiver address validator`() {
        checkExpectedEmptyResult(Validator.validateReceiverAddress(""))
        val incorrectEmailValidationResult = Validator.validateReceiverAddress("123456789")
        incorrectEmailValidationResult.hasError shouldEqualTo true
        incorrectEmailValidationResult.errorMessageId shouldEqualTo R.string.invalid_account_address
        val correctEmailValidationResult = Validator.validateReceiverAddress("0x324324")
        correctEmailValidationResult.hasError shouldEqualTo false
    }

    private fun checkExpectedEmptyResult(result: ValidationResult) {
        result.hasError shouldEqualTo true
        result.errorMessageId shouldEqualTo R.string.field_cannot_be_empty
    }
}