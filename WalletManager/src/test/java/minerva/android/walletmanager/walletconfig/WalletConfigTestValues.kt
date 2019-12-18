package minerva.android.walletmanager.walletconfig

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig

open class WalletConfigTestValues {

    val map1: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "Tom Johnson",
        "Email" to "tj@mail.com",
        "Date of Brith" to "12.09.1991",
        "Some Key" to "Some value",
        "Some Key 2" to "Some value",
        "Some Key 3" to "Some value",
        "Some Key 4" to "Some value"
    )

    val values = listOf(
        Value(0, "", "", "Value 1"),
        Value(1, "", "", "Value 2")
    )
}

class LocalMock : LocalWalletConfigProvider, WalletConfigTestValues() {
    override fun loadWalletConfigRaw(): Observable<String> = Observable.just(prepareData())


    fun prepareData(): String {
        val identities = listOf(
            Identity(0, "", "", "Citizen", map1, false)
        )
        val walletConfig = WalletConfig(identities, values)
        return Gson().toJson(walletConfig)
    }
}

class OnlineMock : OnlineWalletConfigProvider, WalletConfigTestValues() {
    override fun loadWalletConfigRaw(): Single<String> = Single.just(prepareData())

    fun prepareData(): String {
        val identities = listOf(
            Identity(0, "", "", "Citizen2", map1, false)
        )
        val walletConfig = WalletConfig(identities, values)
        return Gson().toJson(walletConfig)
    }
}

class OnlineLikeLocalMock : OnlineWalletConfigProvider, WalletConfigTestValues() {
    override fun loadWalletConfigRaw(): Single<String> = Single.just(prepareData())

    private fun prepareData(): String {
        val identities = listOf(
            Identity(0, "", "", "Citizen", map1, false)
        )
        val walletConfig = WalletConfig(identities, values)
        return Gson().toJson(walletConfig)
    }
}