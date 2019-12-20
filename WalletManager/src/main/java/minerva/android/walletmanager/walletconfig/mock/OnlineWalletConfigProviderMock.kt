package minerva.android.walletmanager.walletconfig.mock

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.walletconfig.OnlineWalletConfigProvider
import java.util.concurrent.TimeUnit

//TODO Will be removed when normal data arrived
class OnlineWalletConfigProviderMock : OnlineWalletConfigProvider {

    override fun loadWalletConfigRaw(): Single<String> =
        Single.just(prepareData()).delay(3, TimeUnit.SECONDS)

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

    fun prepareData(): String {
        val identities = listOf(
            Identity("0", "", "", "Citizen2", map1, false),
            Identity("1", "", "", "Work", map2),
            Identity("2", "", "", "Judo", map3),
            Identity("3", "", "", "Car", map4),
            Identity("4", "", "", "Family", map5),
            Identity("0", "", "", "Citizen", map1),
            Identity("1", "", "", "Work", map2),
            Identity("2", "", "", "Judo", map3),
            Identity("3", "", "", "Car", map4),
            Identity("4", "", "", "Family", map5)
        )
        val values = listOf(
            Value("0", "", "", "Value 1"),
            Value("1", "", "", "Value 2")
        )
        val walletConfig = WalletConfig("0.0", identities, values)
        return Gson().toJson(walletConfig)
    }
}