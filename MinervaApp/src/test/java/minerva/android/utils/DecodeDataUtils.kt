package minerva.android.utils

import com.prettymuchbryce.abidecoder.Decoder
import minerva.android.kotlinUtils.crypto.toHexString
import minerva.android.walletmanager.model.contract.TokenStandardJson
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class DecodeDataUtils {

    @Test
    fun `decode transfer method hex from data`() {
        val data =
            "0xa9059cbb000000000000000000000000e602118e3658a433b60e6f7ced1186fde6df6f5d000000000000000000000000000000000000000000000000000009184e72a000"

        val decoder = Decoder()
        decoder.addAbi(TokenStandardJson.erc20TokenTransactionsAbi)

        val result: Decoder.DecodedMethod? = decoder.decodeMethod(data)
        val value = result?.params?.get(1)?.value
        val name = result?.name

        assertEquals(value, BigInteger.valueOf(10000000000000))
        assertEquals(name, "transfer")
        assertEquals(result?.params?.get(0)?.type, "address")
    }

    @Test
    fun `decode approve method hex from data`() {
        val data =
            "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        val decoder = Decoder()
        decoder.addAbi(TokenStandardJson.erc20TokenTransactionsAbi)

        val result: Decoder.DecodedMethod? = decoder.decodeMethod(data)
        val value = result?.params?.get(1)?.value
        val name = result?.name

        assertEquals(value, BigInteger.valueOf(-1))
        assertEquals(name, "approve")
        assertEquals(result?.params?.get(0)?.type, "address")
    }

    @Test
    fun `decode swap extra tokens for tokens method from hex data`() {
        val data =
            "0x38ed1739000000000000000000000000000000000000000000000000000002ba7def30000000000000000000000000000000000000000000000000000010fc898105daf400000000000000000000000000000000000000000000000000000000000000a000000000000000000000000072f4d6cb761fb9bab743f35f60eb463f3291b4a10000000000000000000000000000000000000000000000000000000060449fa00000000000000000000000000000000000000000000000000000000000000004000000000000000000000000f1738912ae7439475712520797583ac784ea90330000000000000000000000006a023ccd1ff6f2045c3309768ead9e68f978f6e1000000000000000000000000e91d153e0b41518a2ce8dd3d7944fa863463a97d0000000000000000000000008a95ea379e1fa4c749dd0a7a21377162028c479e"

        val decoder = Decoder()
        decoder.addAbi(TokenStandardJson.erc20TokenTransactionsAbi)

        val result: Decoder.DecodedMethod? = decoder.decodeMethod(data)
        val name = result?.name

        assertEquals(result?.params?.get(0)?.value, BigInteger.valueOf(3000000000000))
        assertEquals(name, "swapExactTokensForTokens")
        assertEquals(result?.params?.get(0)?.name, "amountIn")
        assertEquals(result?.params?.get(0)?.type, "uint256")

        assertEquals(result?.params?.get(2)?.type, "address[]")
        assertEquals(result?.params?.get(2)?.name, "path")

        val fromAddress = ((result?.params?.get(2)?.value as Array<*>)[0] as ByteArray).toHexString()
        assertEquals(fromAddress, "f1738912ae7439475712520797583ac784ea9033")

        val toAddress = ((result.params[2].value as Array<*>).last() as ByteArray).toHexString()
        assertEquals(toAddress, "8a95ea379e1fa4c749dd0a7a21377162028c479e")

        assertEquals(result.params[3].type, "address")
        assertEquals(result.params[3].name, "to")
        val addressInBytes: ByteArray = result.params[3].value as ByteArray
        assertEquals(addressInBytes.toHexString(), "72f4d6cb761fb9bab743f35f60eb463f3291b4a1")
    }

    @Test
    fun `decode swap exact tokens for ETH method from hex data`() {
        val data =
            "0x18cbafe5000000000000000000000000000000000000000000000000000000e8d4a510000000000000000000000000000000000000000000000000000000036e09e6d2b900000000000000000000000000000000000000000000000000000000000000a000000000000000000000000072f4d6cb761fb9bab743f35f60eb463f3291b4a1000000000000000000000000000000000000000000000000000000006044de0700000000000000000000000000000000000000000000000000000000000000020000000000000000000000007f7440c5098462f833e123b44b8a03e1d9785bab000000000000000000000000e91d153e0b41518a2ce8dd3d7944fa863463a97d"

        val decoder = Decoder()
        decoder.addAbi(TokenStandardJson.erc20TokenTransactionsAbi)

        val result: Decoder.DecodedMethod? = decoder.decodeMethod(data)
        val name = result?.name

        assertEquals(result?.params?.get(0)?.value, BigInteger.valueOf(1000000000000))
        assertEquals(name, "swapExactTokensForETH")
        assertEquals(result?.params?.get(0)?.name, "amountIn")
        assertEquals(result?.params?.get(0)?.type, "uint256")

        assertEquals(result?.params?.get(2)?.type, "address[]")
        assertEquals(result?.params?.get(2)?.name, "path")

        val fromAddress = ((result?.params?.get(2)?.value as Array<*>)[0] as ByteArray).toHexString()
        assertEquals(fromAddress, "7f7440c5098462f833e123b44b8a03e1d9785bab")

        val toAddress = ((result.params[2].value as Array<*>).last() as ByteArray).toHexString()
        assertEquals(toAddress, "e91d153e0b41518a2ce8dd3d7944fa863463a97d")

        assertEquals(result.params[3].type, "address")
        assertEquals(result.params[3].name, "to")
        val addressInBytes: ByteArray = result.params[3].value as ByteArray
        assertEquals(addressInBytes.toHexString(), "72f4d6cb761fb9bab743f35f60eb463f3291b4a1")
    }
}