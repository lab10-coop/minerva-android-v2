package minerva.android.walletmanager.model

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
            assertEquals(true, getAssetVisibility("network_address_one", "asset_address_one"))
            assertEquals(false, getAssetVisibility("network_address_two", "other_asset_address_one"))
            assertEquals(null, getAssetVisibility("network_address_one", "some_new_asset"))
            assertEquals(null, getAssetVisibility("some_new_network", "some_new_asset"))
            assertEquals(null, getAssetVisibility("network_address_one", "some new asset"))
        }
    }

    @Test
    fun `Check updating visibility asset map`() {
        TokenVisibilitySettings(settingsData).apply {
            assertEquals(true, getAssetVisibility("network_address_one", "asset_address_one"))
            updateAssetVisibility("network_address_one", "asset_address_one", false)
            assertEquals(false, getAssetVisibility("network_address_one", "asset_address_one"))

            assertEquals(null, getAssetVisibility("some_new_network", "some_new_asset"))
            updateAssetVisibility("some_new_network", "some_new_asset", false)
            assertEquals(false, getAssetVisibility("some_new_network", "some_new_asset"))

            assertEquals(false, getAssetVisibility("network_address_two", "other_asset_address_one"))
            updateAssetVisibility("network_address_two", "other_asset_address_one", true)
            assertEquals(true, getAssetVisibility("network_address_two", "other_asset_address_one"))
        }
    }
}