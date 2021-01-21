package minerva.android.walletConnect.storage

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletConnect.model.session.ConnectedDapps
import minerva.android.walletConnect.model.session.Dapp

class WalletConnectStorageImpl(private val sharedPreferences: SharedPreferences) :
    WalletConnectStorage {

    override fun saveDapp(address: String, dapp: Dapp) {
        getDapps().toMutableList().let { list ->
            list.find { item -> item.address == address }?.dapps?.let {
                it.add(dapp)
                sharedPreferences.edit().putString(DAPPS, Gson().toJson(it)).apply()
            }.orElse {
                sharedPreferences.edit().putString(
                    DAPPS,
                    Gson().toJson(listOf(ConnectedDapps(address, dapps = mutableListOf(dapp))))
                ).apply()
            }
        }
    }

    override fun getConnectedDapps(address: String): List<Dapp> =
        getDapps().find { it.address == address }?.dapps ?: listOf()

    override fun removeDapp(address: String, dapp: Dapp) {
        getDapps().toMutableList().let {
            it.find { item -> item.address == address }?.dapps?.remove(dapp)
            sharedPreferences.edit().putString(DAPPS, Gson().toJson(it)).apply()
        }
    }

    override fun clear() {
        sharedPreferences.edit().remove(DAPPS).apply()
    }

    private fun getDapps(): List<ConnectedDapps> {
        sharedPreferences.getString(DAPPS, String.NO_DATA).let { raw ->
            return if (raw == String.NO_DATA) {
                listOf()
            } else {
                Gson().fromJson(raw, object : TypeToken<List<ConnectedDapps>>() {}.type)
            }
        }
    }


    companion object {
        private const val DAPPS = "dapps"
    }
}