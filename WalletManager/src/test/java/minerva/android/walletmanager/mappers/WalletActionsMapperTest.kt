package minerva.android.walletmanager.mappers

import minerva.android.configProvider.model.walletActions.WalletActionClusteredPayload
import minerva.android.configProvider.model.walletActions.WalletActionPayload
import minerva.android.walletmanager.model.mappers.WalletActionsMapper
import org.amshove.kluent.shouldBe
import org.junit.Test

class WalletActionsMapperTest {

    @Test
    fun `test if wallet actions are mapped correctly`() {
        val input = listOf(
            WalletActionClusteredPayload(
                _clusteredActions = mutableListOf(
                    WalletActionPayload(
                        1,
                        2,
                        _fields = hashMapOf(Pair("test", "value"))
                    )
                )
            )
        )
        WalletActionsMapper.map(input).run {
            isNotEmpty() shouldBe true
            get(0).walletActions[0].run {
                status shouldBe 2
                type shouldBe 1
                fields["test"] shouldBe "value"
            }
        }
    }
}