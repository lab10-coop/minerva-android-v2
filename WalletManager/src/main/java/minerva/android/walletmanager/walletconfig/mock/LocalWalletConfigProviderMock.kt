package minerva.android.walletmanager.walletconfig.mock

import com.google.gson.Gson
import io.reactivex.Observable
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.walletconfig.LocalWalletConfigProvider

//TODO Will be removed when normal data arrived
class LocalWalletConfigProviderMock : LocalWalletConfigProvider {

    override fun loadWalletConfigRaw(): Observable<String> =
        //context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(WALLET_CONFIG, EMPTY_DATA) ?: EMPTY_DATA
        Observable.just(prepareData())

    private val map1: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "Tom Johnson",
        "Email" to "tj@mail.com",
        "Date of Brith" to "12.09.1991",
        "Some Key" to "Some value",
        "Some Key 2" to "Some value",
        "Some Key 3" to "Some value",
        "Some Key 4" to "Some value"
    )

    private val map2: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "James Adams",
        "Email" to "ja@email.com",
        "Date of Brith" to "13.03.1974"
    )

    private val map3: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "Jannie Cort",
        "Email" to "jc@emailcom"
    )

    private val map4: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "Michael Knox"
    )

    private val map5: LinkedHashMap<String, String> = linkedMapOf()

    private fun prepareData(): String {
        val identities = listOf(
            Identity(0, "", "", "Citizen", map1, false),
            Identity(1, "", "", "Work", map2),
            Identity(2, "", "", "Judo", map3),
            Identity(3, "", "", "Car", map4),
            Identity(4, "", "", "Family", map5)
        )
        val values = listOf(
            Value(0, "", "", "Value 1"),
            Value(1, "", "", "Value 2")
        )
        val walletConfig = WalletConfig(identities, values)
        return Gson().toJson(walletConfig)
        //return String.NO_DATA
    }
}