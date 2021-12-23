package minerva.android.walletmanager.utils

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `Check correct parsing ipfs url`() {
        val ipfsUrl = "ipfs://testurl"
        val ipfsipfsUrl = "ipfs://ipfs/testurl"
        val nonIpfsUrl = "https://api.com/testurl"
        val ipfsOverHttpUrl = "https://ipfs.io/ipfs/testurl"

        parseIPFSContentUrl(ipfsUrl) shouldBeEqualTo ipfsOverHttpUrl
        parseIPFSContentUrl(ipfsipfsUrl) shouldBeEqualTo ipfsOverHttpUrl
        parseIPFSContentUrl(nonIpfsUrl) shouldBeEqualTo nonIpfsUrl
    }
}