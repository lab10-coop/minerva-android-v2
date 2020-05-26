package minerva.android.validator

import minerva.android.R
import minerva.android.extension.validator.ValidationResult
import minerva.android.extension.validator.Validator
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal

class FormValidatorTest {

    @Test
    fun `test email amount validator`() {
        checkExpectedEmptyResult(Validator.validateAmountField("", BigDecimal(9)))
        val incorrectEmailValidationResult = Validator.validateAmountField("5", BigDecimal(2))
        incorrectEmailValidationResult.hasError shouldBeEqualTo true
        incorrectEmailValidationResult.errorMessageId shouldBeEqualTo R.string.not_enough_funds_error
        val correctEmailValidationResult = Validator.validateAmountField("2", BigDecimal(5))
        correctEmailValidationResult.hasError shouldBeEqualTo false
    }


    @Test
    fun `test receiver address validator`() {
        checkExpectedEmptyResult(Validator.validateReceiverAddress(""))
        val incorrectReceiverValidationResult = Validator.validateReceiverAddress("123456789")
        incorrectReceiverValidationResult.hasError shouldBeEqualTo true
        incorrectReceiverValidationResult.errorMessageId shouldBeEqualTo R.string.invalid_account_address
        val correctReceiverValidationResult = Validator.validateReceiverAddress("0x324324")
        correctReceiverValidationResult.hasError shouldBeEqualTo false
        val correctEnsValidationResult = Validator.validateReceiverAddress("aaa.sfsada")
        correctEnsValidationResult.hasError shouldBeEqualTo false
    }

    private fun checkExpectedEmptyResult(result: ValidationResult) {
        result.hasError shouldBeEqualTo true
        result.errorMessageId shouldBeEqualTo R.string.field_cannot_be_empty
    }
}