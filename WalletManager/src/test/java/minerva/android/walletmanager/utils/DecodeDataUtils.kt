package minerva.android.walletmanager.utils

import com.prettymuchbryce.abidecoder.Decoder
import minerva.android.blockchainprovider.smartContracts.ERC20Json
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class DecodeDataUtils {

    @Test
    fun `decode hex data`() {
        val data =
            "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"


        val decoder = Decoder()
        decoder.addAbi(ERC20Json.json2)

        val result: Decoder.DecodedMethod? = decoder.decodeMethod(data)
        val value = result?.params?.get(0)?.value

        assertEquals(BigInteger("1"), value)
    }
}