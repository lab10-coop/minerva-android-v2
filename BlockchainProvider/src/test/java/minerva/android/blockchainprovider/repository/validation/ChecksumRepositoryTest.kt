package minerva.android.blockchainprovider.repository.validation

import minerva.android.RxTest
import org.junit.Test
import kotlin.test.assertNotEquals

class ChecksumRepositoryTest : RxTest(){
    private val repository = ChecksumRepositoryImpl()
    private val address = "0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359"
    private val eip55Checksum = "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
    private val eip1191Checksum = "0xFb6916095cA1Df60bb79ce92cE3EA74c37c5d359"
    private val rskMainnetChainId = 30
    private val rskTestnetChainId = 31

    @Test
    fun `to address EIP-55 checksum success`() {
        val result = repository.toEIP55Checksum(address)
        kotlin.test.assertEquals(eip55Checksum, result)
    }

    @Test
    fun `to address EIP-1191 checksum success`(){
        val result = repository.toEIP1191Checksum(address, chainId = rskMainnetChainId)
        kotlin.test.assertEquals(eip1191Checksum, result)
    }

    @Test
    fun `is EIP-1991 checksum giving the same result for checksum of address success `() {
        val networks = listOf(rskMainnetChainId, rskTestnetChainId)
        for ( network in networks) {
            val checksum = repository.toEIP1191Checksum(address, chainId = network)
            val result = repository.toEIP1191Checksum(address, chainId = network)
            kotlin.test.assertEquals(checksum, result)
        }
    }

    @Test
    fun `is EIP-55 checksum to address EIP-55 checksum the same success`() {
        val checksum = repository.toEIP55Checksum(address)
        val result = repository.toEIP55Checksum(address)
        kotlin.test.assertEquals(checksum, result)
    }

    @Test
    fun `is address EIP-55 checksum chain different from address EIP-55 checksum success`(){
        val resultWithChainId = repository.toEIP1191Checksum(address, chainId = rskMainnetChainId)
        val result = repository.toEIP55Checksum(address)
        assertNotEquals(resultWithChainId, result)
    }
}