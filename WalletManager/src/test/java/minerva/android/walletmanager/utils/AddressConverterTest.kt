package minerva.android.walletmanager.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Rule
import org.junit.Test

class AddressConverterTest {

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Test
    fun `Check correct short address names`() {
        val address = "0xdaa21bfaa1575e1d725b37ac1efbb7c5f0a32822"
        val did = "did:ethr:0xdaa21bfaa1575e1d725b37ac1efbb7c5f0a32822"
        "0xdaa2...2822" shouldBeEqualTo AddressConverter.getShortAddress(AddressType.NORMAL_ADDRESS, address)
        "did:ethr:0xdaa2...2822" shouldBeEqualTo AddressConverter.getShortAddress(AddressType.DID_ADDRESS, did)
    }
}