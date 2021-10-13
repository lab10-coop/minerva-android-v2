package minerva.android.blockchainprovider.repository.validation

import com.nhaarman.mockitokotlin2.*
import io.mockk.mockk
import minerva.android.RxTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ValidationRepositoryTest : RxTest() {

    private val checksumRepository: ChecksumRepository = mock()
    private val repository = ValidationRepositoryImpl(checksumRepository)
    private val rskMainnetChainId = 30
    private val rskTestnetChainId = 31

    @Test
    fun `is address with checksum valid success test`() {
        whenever(checksumRepository.toEIP55Checksum("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359")).thenReturn("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359")
        val result = repository.isAddressValid("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359")
        assertEquals(true, result)

        whenever(checksumRepository.toEIP1191Checksum("0xFb6916095cA1Df60bb79ce92cE3EA74c37c5d359", rskMainnetChainId)).thenReturn("0xFb6916095cA1Df60bb79ce92cE3EA74c37c5d359")
        val result2 = repository.isAddressValid("0xFb6916095cA1Df60bb79ce92cE3EA74c37c5d359", rskMainnetChainId)
        assertEquals(true, result2)
    }

    @Test
    fun `is address with no checksum and with random big letters invalid success test`() {
        whenever(checksumRepository.toEIP55Checksum("0x9866208bea68B10f04697c00b891541a305Df851")).thenReturn("0x9866208beA68B10F04697C00b891541A305dF851")
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
    fun `to address checksum success`() {
        whenever(checksumRepository.toEIP55Checksum(any())).thenReturn("checksum")
        val result = repository.toChecksumAddress("address")
        assertEquals("checksum", result)
    }

    @Test
    fun `is address checksum with RSK chain id success`() {
        whenever(checksumRepository.toEIP1191Checksum(any(), eq(rskMainnetChainId))).thenReturn("checksum")
        val result1 = repository.toChecksumAddress("address", chainId = rskMainnetChainId)
        assertEquals("checksum", result1)

        whenever(checksumRepository.toEIP1191Checksum(any(), eq(rskTestnetChainId))).thenReturn("checksum")
        val result2 = repository.toChecksumAddress("address", chainId = rskTestnetChainId)
        assertEquals("checksum", result2)
    }

    @Test
    fun `is send transaction within RSK having recipient checksum in lowercase success`() {
        val address = "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
        val lowercase = address.toLowerCase()
        val result = repository.toRecipientChecksum(address, rskMainnetChainId)
        assertEquals(lowercase, result)

        val result2 = repository.toRecipientChecksum(address,rskTestnetChainId)
        assertEquals(lowercase, result2)
    }
}