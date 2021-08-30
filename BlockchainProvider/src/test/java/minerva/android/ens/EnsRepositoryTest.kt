package minerva.android.ens

import io.mockk.every
import io.mockk.mockk
import minerva.android.RxTest
import minerva.android.blockchainprovider.repository.ens.ENSRepositoryImpl
import org.junit.Test
import org.web3j.ens.EnsResolver
import kotlin.test.assertEquals

class EnsRepositoryTest : RxTest() {

    private val ensResolver = mockk<EnsResolver>()
    private val repository = ENSRepositoryImpl(ensResolver)

    @Test
    fun `is address with checksum valid success test`() {
        val result = repository.isAddressValid("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359")
        assertEquals(true, result)
    }

    @Test
    fun `is address with no checksum and with random big letters invalid success test`() {
        val result = repository.isAddressValid("0x9866208bea68B10f04697c00b891541a305Df851")
        assertEquals(false, result)
    }

    @Test
    fun `is address with no checksum but with all small letters success test`() {
        val result = repository.isAddressValid("0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359")
        assertEquals(true, result)
    }

    @Test
    fun `is address valid fail test`() {
        val result = repository.isAddressValid("address")
        assertEquals(false, result)
    }

    @Test
    fun `to address checksum test`() {
        val result = repository.toChecksumAddress("0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359")
        assertEquals("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359", result)
    }

    @Test
    fun `resolve ens name test`() {
        every { ensResolver.resolve(any()) } returns "tom"
        repository.resolveENS("tom.eth")
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it == "tom"
            }
    }

    @Test
    fun `reverse resolver ens`() {
        every { ensResolver.reverseResolve(any()) } returns "tom.eth"
        repository.reverseResolveENS("0x12332423")
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it == "tom.eth"
            }
    }

    @Test
    fun `resolve normal name test`() {
        repository.resolveENS("tom")
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it == "tom"
            }
    }
}