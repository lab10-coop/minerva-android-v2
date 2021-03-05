package minerva.android.walletmanager.utils

import com.prettymuchbryce.abidecoder.Decoder
import minerva.android.blockchainprovider.smartContracts.TokenStandardJson
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class DecodeDataUtils {


//    0x7ff36ab5000000000000000000000000000000000000000000000000036ad151ee54fe8d000000000000000000000000000000000000000000000000000000000000008000000000000000000000000072f4d6cb761fb9bab743f35f60eb463f3291b4a1000000000000000000000000000000000000000000000000000000006040fc7b0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000e91d153e0b41518a2ce8dd3d7944fa863463a97d0000000000000000000000007f7440c5098462f833e123b44b8a03e1d9785bab
    @Test
    fun `decode transfer method hex data`() {
        val data =
            "0xa9059cbb000000000000000000000000e602118e3658a433b60e6f7ced1186fde6df6f5d000000000000000000000000000000000000000000000000000009184e72a000"

        val decoder = Decoder()
        decoder.addAbi(TokenStandardJson.erc20Abi)

        val result: Decoder.DecodedMethod? = decoder.decodeMethod(data)
        val value = result?.params?.get(1)?.value
        val name = result?.name

        assertEquals(value, BigInteger.valueOf(10000000000000))
        assertEquals(name, "transfer")
        assertEquals(result?.params?.get(0)?.type, "address")
    }

    @Test
    fun `decode approve method hex data`() {
        val data =
            "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        val decoder = Decoder()
        decoder.addAbi(TokenStandardJson.erc20Abi)

        val result: Decoder.DecodedMethod? = decoder.decodeMethod(data)
        val value = result?.params?.get(1)?.value
        val name = result?.name

        assertEquals(value, BigInteger.valueOf(-1))
        assertEquals(name, "approve")
        assertEquals(result?.params?.get(0)?.type, "address")
    }
}