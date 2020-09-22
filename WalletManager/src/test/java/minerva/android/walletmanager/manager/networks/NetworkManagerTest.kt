package minerva.android.walletmanager.manager.networks


import minerva.android.walletmanager.exception.NoActiveNetworkThrowable
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Network
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertFails
import kotlin.test.assertTrue

class NetworkManagerTest {

    @Test
    fun `Check initialization done with correct order`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.networks[0].https shouldBeEqualTo "someAddress1"
        NetworkManager.networks[1].https shouldBeEqualTo "someAddress3"
        NetworkManager.networks[2].https shouldBeEqualTo "someAddress5"
        NetworkManager.networks[3].https shouldBeEqualTo ""
        NetworkManager.networks[4].https shouldBeEqualTo ""
    }

    @Test
    fun `Check from string works properly`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.getNetwork("SN1").full shouldBeEqualTo "FullName1"
        NetworkManager.getNetwork("SN3").full shouldBeEqualTo "FullName3"
        val exception = assertFails { NetworkManager.getNetwork("###") }
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
        NetworkManager.firstDefaultValueNetwork().full shouldBeEqualTo "FullName1"
        NetworkManager.initialize(testNetworks2)
        NetworkManager.firstDefaultValueNetwork().full shouldBeEqualTo "FullName3"
    }

    @Test
    fun `Check getting second default Account network correctly`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.secondDefaultValueNetwork().full shouldBeEqualTo "FullName3"
        NetworkManager.initialize(testNetworks2)
        NetworkManager.secondDefaultValueNetwork().full shouldBeEqualTo "FullName3"
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
        NetworkManager.getStringColor("SN1", false) shouldBeEqualTo "#FF223344"
        NetworkManager.getStringColor("SN1", true) shouldBeEqualTo "#29223344"
        NetworkManager.getStringColor("SN2", false) shouldBeEqualTo "#223344"
        NetworkManager.getStringColor("SN2", true) shouldBeEqualTo "#29223344"
    }

    @Test
    fun `Getting correct assets`() {
        NetworkManager.initialize(testNetworks1)
        NetworkManager.getAllAsset().size shouldBeEqualTo 6
        NetworkManager.initialize(testNetworks2)
        NetworkManager.getAllAsset().size shouldBeEqualTo 0
    }

    private val assetSet1 = listOf(
        Asset("asset1", "ass1", "address1"),
        Asset("asset2", "ass2", "address2"),
        Asset("asset3", "ass3", "address3")
    )

    private val assetSet2 = listOf(
        Asset("asset4", "ass4", "address4"),
        Asset("asset5", "ass5", "address5"),
        Asset("asset6", "ass6", "address6")
    )

    private val testNetworks1 = listOf(
        Network("FullName1", "SN1", "WT1", "someAddress1", "someAddress1", false, BigInteger.TEN, assetSet1, "#FF223344"),
        Network("FullName2", "SN2", "WT2", "", "someAddress2", false, BigInteger.TEN, assetSet2, "#223344"),
        Network("FullName3", "SN3", "WT3", "someAddress3", "someAddress3", false, BigInteger.TEN, listOf(), "#FF223344"),
        Network("FullName4", "SN4", "WT4", "", "someAddress4", true, BigInteger.TEN, listOf(), "#223344"),
        Network("FullName5", "SN5", "WT5", "someAddress5", "someAddress5", true, BigInteger.TEN, listOf(), "#FF223344")
    )

    private val testNetworks2 = listOf(
        Network("FullName1", "SN1", "WT1", "", "", false, BigInteger.TEN, listOf(), "#223344"),
        Network("FullName2", "SN2", "WT2", "", "", false, BigInteger.TEN, listOf(), "#FF223344"),
        Network("FullName3", "SN3", "WT3", "someAddress3", "someAddress4", false, BigInteger.TEN, listOf(), "#FF223344"),
        Network("FullName4", "SN4", "WT4", "", "", true, BigInteger.TEN, listOf(), "#223344"),
        Network("FullName5", "SN5", "WT5", "", "", true, BigInteger.TEN, listOf(), "#223344")
    )

    private val testNetworks3 = listOf(
        Network("FullName1", "SN1", "WT1", "", "", false, BigInteger.TEN, listOf(), "#223344"),
        Network("FullName2", "SN2", "WT2", "", "", false, BigInteger.TEN, listOf(), "#FF223344"),
        Network("FullName3", "SN3", "WT3", "", "", false, BigInteger.TEN, listOf(), "#223344"),
        Network("FullName4", "SN4", "WT4", "", "", true, BigInteger.TEN, listOf(), "#223344"),
        Network("FullName5", "SN5", "WT5", "", "", true, BigInteger.TEN, listOf(), "#223344")
    )

    private val testNetworks4 = listOf<Network>()
}