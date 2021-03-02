package minerva.android.walletmanager.manager.networks


import minerva.android.walletmanager.exception.NoActiveNetworkThrowable
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.token.ERC20Token
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertFails
import kotlin.test.assertTrue

class NetworkManagerTest {

    @Test
    fun `Check initialization done with correct order`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.networks[0].httpRpc shouldBeEqualTo "someAddress1"
        NetworkManager.networks[1].httpRpc shouldBeEqualTo "someAddress3"
        NetworkManager.networks[2].httpRpc shouldBeEqualTo "someAddress5"
        NetworkManager.networks[3].httpRpc shouldBeEqualTo ""
        NetworkManager.networks[4].httpRpc shouldBeEqualTo ""
    }

    @Test
    fun `Check from string works properly`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.getNetwork(1).name shouldBeEqualTo "FullName1"
        NetworkManager.getNetwork(3).name shouldBeEqualTo "FullName3"
        val exception = assertFails { NetworkManager.getNetwork(-1) }
        exception.message shouldBeEqualTo "Value not found"
    }

    @Test
    fun `Check that checking safe account availability`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.networks[0].isSafeAccountAvailable shouldBeEqualTo false
        NetworkManager.networks[1].isSafeAccountAvailable shouldBeEqualTo false
        NetworkManager.networks[2].isSafeAccountAvailable shouldBeEqualTo true
        NetworkManager.networks[3].isSafeAccountAvailable shouldBeEqualTo false
        NetworkManager.networks[4].isSafeAccountAvailable shouldBeEqualTo true
    }

    @Test
    fun `Check getting first default Account network correctly`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.firstDefaultValueNetwork().name shouldBeEqualTo "FullName1"
        NetworkManager.initialize(testNetworks2)
        NetworkManager.firstDefaultValueNetwork().name shouldBeEqualTo "FullName3"
    }

    @Test
    fun `Check getting second default Account network correctly`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.getNetworkByIndex(2).name shouldBeEqualTo "FullName5"
        NetworkManager.initialize(testNetworks2)
        NetworkManager.getNetworkByIndex(2).name shouldBeEqualTo "FullName3"
        NetworkManager.getNetworkByIndex(3).name shouldBeEqualTo "FullName3"
    }

    @Test
    fun `Check passing wrong config file`() {
        val exceptionNoActive = assertFails { NetworkManager.initialize(testNetworks3) }
        assertTrue { exceptionNoActive is NoActiveNetworkThrowable }
        val exceptionEmpty = assertFails { NetworkManager.initialize(testNetworks4) }
        assertTrue { exceptionEmpty is NoActiveNetworkThrowable }
    }

    @Test
    fun `Getting correct color`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.getStringColor(testNetworks1[0], false) shouldBeEqualTo "#FF223344"
        NetworkManager.getStringColor(testNetworks1[0], true) shouldBeEqualTo "#29223344"
        NetworkManager.getStringColor(testNetworks1[1], false) shouldBeEqualTo "#223344"
        NetworkManager.getStringColor(testNetworks1[1], true) shouldBeEqualTo "#29223344"
    }
    
    @Test
    fun `Getting correct tokens for network`() {
        NetworkManager.initialize(testNetworks1)
        val tokensSet1 = NetworkManager.getTokens(1)
        tokensSet1.size shouldBeEqualTo 3
        tokensSet1[0].name shouldBeEqualTo "token1"
        val tokensSet2 = NetworkManager.getTokens(2)
        tokensSet2.size shouldBeEqualTo 3
        tokensSet2[0].name shouldBeEqualTo "token4"
        val tokensSet3 = NetworkManager.getTokens(3)
        tokensSet3.size shouldBeEqualTo 0
        val tokenSet4 = NetworkManager.getTokens(0)
        tokenSet4.size shouldBeEqualTo 0
    }

    private val tokenSet1 = listOf(
        ERC20Token(2, "token1", "ass1", "address1"),
        ERC20Token(2, "token2", "ass2", "address2"),
        ERC20Token(2, "token3", "ass3", "address3")
    )

    private val tokenSet2 = listOf(
        ERC20Token(3, "token4", "ass4", "address4"),
        ERC20Token(3, "token5", "ass5", "address5"),
        ERC20Token(3, "token6", "ass6", "address6")
    )

    private val testNetworks1 = listOf(
        Network("FullName1", "WT1", "someAddress1", "someAddress1", false, BigInteger.TEN, "", tokenSet1, "#FF223344", chainId = 1),
        Network("FullName2", "WT2", "", "someAddress2", false, BigInteger.TEN, "", tokenSet2, "#223344", chainId = 2),
        Network("FullName3", "WT3", "someAddress3", "someAddress3", false, BigInteger.TEN, "", listOf(), "#FF223344", chainId = 3),
        Network("FullName4", "WT4", "", "someAddress4", true, BigInteger.TEN, "", listOf(), "#223344", false, chainId = 4),
        Network("FullName5", "WT5", "someAddress5", "someAddress5", true, BigInteger.TEN, "", listOf(), "#FF223344", false, chainId = 5)
    )

    private val testNetworks2 = listOf(
        Network("FullName1", "WT1", "", "", false, BigInteger.TEN, "", listOf(), "#223344", chainId = 1),
        Network("FullName2", "WT2", "", "", false, BigInteger.TEN, "", listOf(), "#FF223344", chainId = 2),
        Network("FullName3", "WT3", "someAddress3", "someAddress4", false, BigInteger.TEN, "", listOf(), "#FF223344", chainId = 3),
        Network("FullName4", "WT4", "", "", true, BigInteger.TEN, "", listOf(), "#223344", chainId = 4),
        Network("FullName5", "WT5", "", "", true, BigInteger.TEN, "", listOf(), "#223344", chainId = 5)
    )

    private val testNetworks3 = listOf(
        Network("FullName1", "WT1", "", "", false, BigInteger.TEN, "", listOf(), "#223344", chainId = 1),
        Network("FullName2", "WT2", "", "", false, BigInteger.TEN, "", listOf(), "#FF223344", chainId = 2),
        Network("FullName3", "WT3", "", "", false, BigInteger.TEN, "", listOf(), "#223344", chainId = 3),
        Network("FullName4", "WT4", "", "", true, BigInteger.TEN, "", listOf(), "#223344", chainId = 4),
        Network("FullName5", "WT5", "", "", true, BigInteger.TEN, "", listOf(), "#223344", chainId = 5)
    )

    private val testNetworks4 = listOf<Network>()
}