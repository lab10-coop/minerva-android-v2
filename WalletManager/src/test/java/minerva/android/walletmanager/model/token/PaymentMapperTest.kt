package minerva.android.walletmanager.model.token

import minerva.android.walletmanager.model.defs.PaymentRequest
import minerva.android.walletmanager.model.mappers.PaymentMapper
import org.amshove.kluent.shouldBe
import org.junit.Test

class PaymentMapperTest {

    @Test
    fun `test if payment is mapped correctly from map`() {

        val input = mapOf(
            Pair(PaymentRequest.AMOUNT, "10"),
            Pair(PaymentRequest.IBAN, "iban"),
            Pair(PaymentRequest.RECIPIENT, "recipient"),
            Pair(PaymentRequest.SERVICE_NAME, "name"),
            Pair(PaymentRequest.SERVICE_SHORT_NAME, "short"),
            Pair(PaymentRequest.URL, "url")
        )

        PaymentMapper.map(input).run {
            amount shouldBe "10"
            iban shouldBe "iban"
            recipient shouldBe "recipient"
            serviceName shouldBe "name"
            shortName shouldBe "short"
            url shouldBe "url"
        }
    }
}