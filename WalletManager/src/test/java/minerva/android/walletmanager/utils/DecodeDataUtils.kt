package minerva.android.walletmanager.utils

import com.prettymuchbryce.abidecoder.Decoder
import minerva.android.blockchainprovider.smartContracts.TokenStandardJson
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class DecodeDataUtils {

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
}