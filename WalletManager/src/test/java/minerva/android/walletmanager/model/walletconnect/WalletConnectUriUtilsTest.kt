package minerva.android.walletmanager.model.walletconnect

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class WalletConnectUriUtilsTest {
    @Test
    fun `walletConnectVersionFromUri should return 1 for v1 uri`() {
        val uri = "wc:abcdefgh@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=1234"
        val expected = 1
        val actual = WalletConnectUriUtils.walletConnectVersionFromUri(uri)
        actual shouldBeEqualTo expected
    }

    @Test
    fun `walletConnectVersionFromUri should return 2 for v2 uri`() {
        val uri = "wc:abcdefgh@2?symKey=1234&relay-protocol=https%3A%2F%2Frelay.walletconnect.org"
        val expected = 2
        val actual = WalletConnectUriUtils.walletConnectVersionFromUri(uri)
        actual shouldBeEqualTo expected
    }

    @Test
    fun `isValidWalletConnectUri should return true for valid v1 uri`() {
        val validV1Uris = listOf(
            "wc:abcdefgh@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=1234",
            "wc:abcdefgh@1?key=1234&bridge=https%3A%2F%2Fbridge.walletconnect.org",
            "wc:abcdefgh@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=1234&extraparam=extravalue",
            "wc:ac9e9b4d-1eac-4d88-b832-eadc9680c2a4@1?bridge=https%3A%2F%2Fg.bridge.walletconnect.org&key=be3c056d33ad136c11dcb856776d81fb32bf4029ae6d2aaf13148490ec32b559",
            "wc:ce71b92e-735f-4a80-bba4-e89975ef4c73@1?bridge=https%3A%2F%2F5.bridge.walletconnect.org&key=daa904f3e0f9f892a2882686dacb5ad2c74fa65dbad983434a009983f40655e5"
        )
        validV1Uris.forEach {
            WalletConnectUriUtils.isValidWalletConnectUri(it) shouldBeEqualTo true
        }
    }

    @Test
    fun `isValidWalletConnectUri should return true for valid v2 uri`() {
        val validV2Uris = listOf(
            "wc:abcdefgh@2?symKey=1234&relay-protocol=https%3A%2F%2Frelay.walletconnect.org",
            "wc:abcdefgh@2?relay-protocol=https%3A%2F%2Frelay.walletconnect.org&symKey=1234",
            "wc:abcdefgh@2?relay-protocol=https%3A%2F%2Frelay.walletconnect.org&symKey=1234&extraparam=extravalue",
            "wc:9bf07add837248dcfabc84b30ca27be080f08c016af43396a694c5c207d6944a@2?relay-protocol=irn&symKey=666a8ae4dfd28b251c37806b8cad16493a569779adacbce49f876525ba59ecd4",
            "wc:f8ac125299ec9d1c7fcea185d4bc3da7bf30ec36fbef401d84920bbfbd7cafbe@2?relay-protocol=irn&symKey=ae3fdb3d3a2a3aee6f84fcca811745a804b9a1aff3725682d9cdf24c3eaf0f37"
        )
        validV2Uris.forEach {
            WalletConnectUriUtils.isValidWalletConnectUri(it) shouldBeEqualTo true
        }
    }

    @Test
    fun `isValidWalletConnectUri should return false for invalid uri`() {
        val uri = "wc:abcdefgh@2"
        val expected = false
        val actual = WalletConnectUriUtils.isValidWalletConnectUri(uri)
        actual shouldBeEqualTo expected
    }
}
