package minerva.android.walletmanager.model.mappers

import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.transactions.Payment
import minerva.android.walletmanager.model.defs.PaymentRequest

object PaymentMapper : Mapper<Map<String, Any?>, Payment> {
    override fun map(input: Map<String, Any?>): Payment =
        Payment(
            input[PaymentRequest.AMOUNT] as String,
            input[PaymentRequest.IBAN] as String,
            input[PaymentRequest.RECIPIENT] as String,
            input[PaymentRequest.SERVICE_NAME] as String,
            input[PaymentRequest.SERVICE_SHORT_NAME] as String,
            input[PaymentRequest.URL] as String
        )
}