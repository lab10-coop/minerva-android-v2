package minerva.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepositoryImpl
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class CryptographyRepositoryTest {

    private val repository: CryptographyRepository = CryptographyRepositoryImpl()

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `get mnemonic for master seed`(){
        val test = repository.getMnemonicForMasterSeed("68a4c6de013faef9b98d7d3e2546ce07")
        assertEquals(test, "hamster change resource act wife lamp tower quick dilemma clay receive attract")
    }

    @Test
    fun `compute derived keys test`(){
        val test = repository.computeDeliveredKeys("68a4c6de013faef9b98d7d3e2546ce07", 1).test()
        test.assertValue {
            it.index == 1 &&
            it.address == "0xe82aded79d7af28aa6664d6fc009e3485d0f6d75"
        }
    }

    @Test
    fun `crate master seed test`(){
        val test = repository.createMasterSeed().test()
        test.assertValue { it.first.isNotEmpty() && it.second.isNotEmpty() && it.third.isNotEmpty() }
    }

    @Test
    fun `restore master seed test`(){
        val test = repository.restoreMasterSeed("hamster change resource act wife lamp tower quick dilemma clay receive attract").test()
        test.assertValue { it.first == "68a4c6de013faef9b98d7d3e2546ce07" }
    }

    @Test
    fun `test mnemonic validator`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, emptyList())
    }

    @Test
    fun `test mnemonic validator collecting invalid words`() {
        val mnemonic = "vessel *$ alter error federal HEHE chat ability sun Test valve picture"
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, listOf("*$", "HEHE", "Test"))
    }

    @Test
    fun `test mnemonic validator when mnemonic is empty`() {
        val mnemonic = ""
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, emptyList())
    }

    @Test
    fun `test mnemonic validator when mnemonic has blank spaces`() {
        val mnemonic = "     "
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, emptyList())
    }

    @Test
    fun `test mnemonic validator when mnemonic is too short and has invalid words`() {
        val mnemonic = "vessel error federal aaaaa sibling chat ability kkkkk sun glass valve picture"
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, listOf("aaaaa", "kkkkk"))
    }
}