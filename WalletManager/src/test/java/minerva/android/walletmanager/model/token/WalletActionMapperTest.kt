package minerva.android.walletmanager.model.token

import minerva.android.configProvider.model.walletActions.WalletActionPayload
import minerva.android.walletmanager.model.mappers.WalletActionMapper
import org.amshove.kluent.shouldBe
import org.junit.Test

class WalletActionMapperTest {

    @Test
    fun `test if wallet action mapper maps wallet action correctly`() {
        val walletActionPayload = WalletActionPayload(1, 2, _fields = hashMapOf(Pair("test", "value")))
        WalletActionMapper.map(walletActionPayload).run {
            type shouldBe 1
            status shouldBe 2
            fields["test"] shouldBe "value"
        }
    }
}