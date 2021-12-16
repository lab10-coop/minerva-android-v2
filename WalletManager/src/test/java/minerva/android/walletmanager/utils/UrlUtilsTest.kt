package minerva.android.walletmanager.utils

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `Check correct parsing ipfs url`() {
        val ipfsUrl = "ipfs://testurl"
        val nonIpfsUrl = "https://api.com/testurl"
        "https://ipfs.io/ipfs/testurl" shouldBeEqualTo parseIPFSContentUrl(ipfsUrl)
        nonIpfsUrl shouldBeEqualTo parseIPFSContentUrl(nonIpfsUrl)
    }
}