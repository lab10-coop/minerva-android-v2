package minerva.android.accounts.address

import minerva.android.accounts.transaction.fragment.scanner.AddressParser
import org.junit.Test
import kotlin.test.assertEquals

class AddressParserTest {

    @Test
    fun `get address with prefix test`() {
        val address = "ethereum:0x344423432"
        val result = AddressParser.parse(address)
        assertEquals("0x344423432", result)
    }

    @Test
    fun `get address when no prefix test`() {
        val address = "0x344423432"
        val result = AddressParser.parse(address)
        assertEquals("0x344423432", result)
    }

    @Test
    fun `get address when only separator test`() {
        val address = ":0x344423432"
        val result = AddressParser.parse(address)
        assertEquals("0x344423432", result)
    }

    @Test
    fun `get address when more parts test`() {
        val address = "test:eth:0x344423432"
        val result = AddressParser.parse(address)
        assertEquals("test:eth:0x344423432", result)
    }

    @Test
    fun `scan wc url test with many parts`(){
        val address = "wc:test:eth:0x344423432"
        val result = AddressParser.parse(address)
        assertEquals("wc", result)
    }

    @Test
    fun `scan wc url test with two parts`(){
        val address = "wc:0x344423432"
        val result = AddressParser.parse(address)
        assertEquals("wc", result)
    }
}