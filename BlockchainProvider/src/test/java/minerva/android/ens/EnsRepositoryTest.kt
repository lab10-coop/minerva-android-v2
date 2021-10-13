package minerva.android.ens

import io.mockk.every
import io.mockk.mockk
import minerva.android.RxTest
import minerva.android.blockchainprovider.repository.ens.ENSRepositoryImpl
import org.junit.Test
import org.web3j.ens.EnsResolver
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EnsRepositoryTest : RxTest() {

    private val ensResolver = mockk<EnsResolver>()
    private val repository = ENSRepositoryImpl(ensResolver)

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