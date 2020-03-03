package minerva.android.walletmanager.mappers

import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.mappers.payloads.WalletActionPayloadMapper
import org.amshove.kluent.shouldBe
import org.junit.Test

class WalletActionPayloadMapperTest {
    @Test
    fun `test in wallet action payload is mapped correctly`() {
        val walletAction = WalletAction(1, 2, fields = hashMapOf(Pair("test", "value")))
        WalletActionPayloadMapper.map(walletAction).run {
            type shouldBe 1
            status shouldBe 2
            fields["test"] shouldBe "value"
        }
    }
}