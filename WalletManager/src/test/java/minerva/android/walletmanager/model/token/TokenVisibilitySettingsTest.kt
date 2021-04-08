package minerva.android.walletmanager.model.token

import org.junit.Test
import kotlin.test.assertEquals

class TokenVisibilitySettingsTest {

    private val settingsData: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf(
        Pair(
            "network_address_one", mutableMapOf(
                Pair("asset_address_one", true),
                Pair("asset_address_two", false)
            )
        ),
        Pair(
            "network_address_two", mutableMapOf(
                Pair("other_asset_address_one", false),
                Pair("other_asset_address_two", true)
            )
        )
    )

    @Test
    fun `Check Asset visibility`() {
        TokenVisibilitySettings(settingsData).apply {
            assertEquals(true, getTokenVisibility("network_address_one", "asset_address_one"))
            assertEquals(false, getTokenVisibility("network_address_two", "other_asset_address_one"))
            assertEquals(null, getTokenVisibility("network_address_one", "some_new_asset"))
            assertEquals(null, getTokenVisibility("some_new_network", "some_new_asset"))
            assertEquals(null, getTokenVisibility("network_address_one", "some new asset"))
        }
    }

    @Test
    fun `Check updating visibility asset map`() {
        TokenVisibilitySettings(settingsData).apply {
            assertEquals(true, getTokenVisibility("network_address_one", "asset_address_one"))
            updateTokenVisibility("network_address_one", "asset_address_one", false)
            assertEquals(false, getTokenVisibility("network_address_one", "asset_address_one"))

            assertEquals(null, getTokenVisibility("some_new_network", "some_new_asset"))
            updateTokenVisibility("some_new_network", "some_new_asset", false)
            assertEquals(false, getTokenVisibility("some_new_network", "some_new_asset"))

            assertEquals(false, getTokenVisibility("network_address_two", "other_asset_address_one"))
            updateTokenVisibility("network_address_two", "other_asset_address_one", true)
            assertEquals(true, getTokenVisibility("network_address_two", "other_asset_address_one"))
        }
    }
}