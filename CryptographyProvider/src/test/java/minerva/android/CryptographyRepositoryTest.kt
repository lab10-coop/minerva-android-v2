package minerva.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import me.uport.sdk.jwt.InvalidJWTException
import me.uport.sdk.jwt.JWTTools
import me.uport.sdk.jwt.model.JwtHeader
import me.uport.sdk.jwt.model.JwtPayload
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepositoryImpl
import minerva.android.cryptographyProvider.repository.model.SeedError
import minerva.android.cryptographyProvider.repository.model.SeedWithKeys
import minerva.android.cryptographyProvider.repository.throwable.InvalidJwtThrowable
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class CryptographyRepositoryTest {

    private val jwtTools: JWTTools = Mockito.mock(JWTTools::class.java)
    private val repository: CryptographyRepository = CryptographyRepositoryImpl(jwtTools)

    private val masterKeysPath = "m/"
    private val didPath = "m/73'/0'/0'/"
    private val mainNetPath = "m/44'/60'/0'/0/"
    private val testNetPath = "m/44'/1'/0'/0/"
    private val token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJleHAiOjE2MDkzNzI4MDAsInZjIjp7ImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImF1dG9tb3RpdmVNZW1iZXJzaGlwQ2FyZCI6eyJjcmVkZW50aWFsTmFtZSI6IsOWQU1UQyBDbHVia2FydGUiLCJjYXJkSW1hZ2UiOnsiLyI6Ii9pcGZzL1FtWXBaMUxCQlJhZDVpUWZhWm9LQUZBTmg0Z3k0MXozb0J4UUtnd3lRY3hFeXIvIn0sImljb25JbWFnZSI6eyIvIjoiL2lwZnMvUW1TdXlmd1d0Q3d3Y1Jlb1dIVXc0bXJSWEdlelUyS0RSaVBpclNGaDV3ZFVKTi8ifSwibWVtYmVySWQiOiIxMiAzNDUgNjc4IiwibmFtZSI6Ik1zLiBSYW5kb20iLCJzaW5jZSI6IjIwMTAiLCJjb3ZlcmFnZSI6IkF1dG8ifX0sIkBjb250ZXh0IjpbImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL3YxIiwiaHR0cHM6Ly9zY2hlbWEuZGV2LmxhYjEwLmlvL0F1dG9tb3RpdmVNZW1iZXJzaGlwQ2FyZENyZWRlbnRpYWwuanNvbiJdLCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiQXV0b21vdGl2ZU1lbWJlcnNoaXBDYXJkQ3JlZGVudGlhbCJdfSwic3ViIjoiIiwibmJmIjoxNjA1NzA2ODM3LCJpc3MiOiJkaWQ6ZXRocjphcnRpc190MToweDI2MDViZjhjOWI5M2JkMjM5YjYzMGIxMjVjZjk3NzBkYjBlZDU1MDEifQ.Al6FZKfmG2z9hnjgVixOsmpIYJUO-BqYlXfWJyau_oTA-gwOLTZ7ihqb31IAPPJK49Qz8qaw8nR64F-HAJzewQ"

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
    fun `get mnemonic for master seed`() {
        val test = repository.getMnemonicForMasterSeed("68a4c6de013faef9b98d7d3e2546ce07")
        assertEquals(test, "hamster change resource act wife lamp tower quick dilemma clay receive attract")
    }

    @Test
    fun `compute derived keys for identities test`() {
        val test = repository.calculateDerivedKeysSingle("68a4c6de013faef9b98d7d3e2546ce07", 1, didPath).test()
        test.assertValue {
            it.index == 1 && it.address == "0x94c87a5f423dbe7bbb085a963142cfd12e6c001e"
        }
    }

    @Test
    fun `compute derived keys for test nets test`() {
        val test = repository.calculateDerivedKeysSingle("68a4c6de013faef9b98d7d3e2546ce07", 1, testNetPath).test()
        test.assertValue {
            it.index == 1 && it.address == "0x4ecc9dbd0494b32bbd77c87f46da92ff5f0c2258"
        }
    }

    @Test
    fun `compute derived keys for main nets test`() {
        val test = repository.calculateDerivedKeysSingle("68a4c6de013faef9b98d7d3e2546ce07", 1, mainNetPath).test()
        test.assertValue {
            it.index == 1 && it.address == "0x1e7cfbf30f2ae071806a78135f0c1280dece8fda"
        }
    }

    @Test
    fun `crate master seed test`() {
        val test = repository.createMasterSeed().test()
        test.assertValue { it.first.isNotEmpty() && it.second.isNotEmpty() && it.third.isNotEmpty() }
    }

    @Test
    fun `restore master seed test`() {
        val result =
            repository.restoreMasterSeed("hamster change resource act wife lamp tower quick dilemma clay receive attract")
        (result as SeedWithKeys).apply {
            seed shouldBeEqualTo "68a4c6de013faef9b98d7d3e2546ce07"
        }
    }

    @Test
    fun `restore master seed error test`() {
        val result =
            repository.restoreMasterSeed("dasdasd change resource act wife lamp tower quick dilemma clay receive attract")
        (result as SeedError).apply {
            error.message shouldBeEqualTo "word(dasdasd) not in known word list"
        }
    }

    @Test
    fun `test mnemonic validator`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        val validation = repository.areMnemonicWordsValid(mnemonic)
        assertEquals(validation, true)
    }

    @Test
    fun `test mnemonic validator collecting invalid words`() {
        val mnemonic = "vessel *$ alter error federal HEHE chat ability sun Test valve picture"
        val validation = repository.areMnemonicWordsValid(mnemonic)
        assertEquals(validation, false)
    }

    @Test
    fun `test mnemonic validator when mnemonic is empty`() {
        val mnemonic = ""
        val validation = repository.areMnemonicWordsValid(mnemonic)
        assertEquals(validation, true)
    }

    @Test
    fun `test mnemonic validator when mnemonic has blank spaces`() {
        val mnemonic = "     "
        val validation = repository.areMnemonicWordsValid(mnemonic)
        assertEquals(validation, true)
    }

    @Test
    fun `test mnemonic validator when mnemonic is too short and has invalid words`() {
        val mnemonic = "vessel error federal aaaaa sibling chat ability kkkkk sun glass valve picture"
        val validation = repository.areMnemonicWordsValid(mnemonic)
        assertEquals(validation, false)
    }

    @Test
    fun `create jwt token success test`() {
        runBlocking { whenever(jwtTools.createJWT(any(), any(), any(), any(), any())).doReturn("token") }
        repository.createJwtToken(mapOf(), "0x94c87a5f423dbe7bbb085a963142cfd12e6c001e")
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it == "token"
            }
    }

    @Test
    fun `decode jwt token success test`() {
        runBlocking { whenever(jwtTools.verify(any(), any(), any(), any())).doReturn(JwtPayload()) }
        runBlocking { whenever(jwtTools.decodeRaw(any())).doReturn(Triple(JwtHeader(), mapOf("test" to "key"), ByteArray(1))) }
        repository.decodeJwtToken(token)
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it["test"] == "key"
            }
    }

    @Test
    fun `decode jwt token error test`() {
        val error = InvalidJWTException("Invalid JWT Exception")
        runBlocking { whenever(jwtTools.verify(any(), any(), any(), any())).doReturn(JwtPayload()) }
        runBlocking { whenever(jwtTools.decodeRaw(any())).thenThrow(error) }
        repository.decodeJwtToken("token")
            .test()
            .await()
            .assertError {
                it is InvalidJwtThrowable
            }
    }

    @Test
    fun `decode jwt token invalid argument error test`() {
        val invalidJwt = "invalidJWT"
        val error = IllegalArgumentException("Invalid JWT Exception")
        runBlocking { whenever(jwtTools.verify(any(), any(), any(), any())).doReturn(JwtPayload()) }
        runBlocking { whenever(jwtTools.decodeRaw(any())).thenThrow(error) }
        repository.decodeJwtToken(invalidJwt)
            .test()
            .await()
            .assertError {
                it is InvalidJwtThrowable
            }
    }
}