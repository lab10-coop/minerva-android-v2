package minerva.android.walletmanager.manager.networks

import android.content.Context
import androidx.startup.Initializer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.Network
import java.io.IOException

class NetworkInitializer : Initializer<NetworkManager> {
    override fun create(context: Context): NetworkManager {
        initializeNetworkData(context).let{
            NetworkManager.initialize(it)
        }
        return NetworkManager
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun initializeNetworkData(context: Context): List<Network> {
        val listNetworkType = object : TypeToken<List<Network>>() {}.type
        return Gson().fromJson(getNetworkRAWData(context), listNetworkType)
    }

    private fun getNetworkRAWData(context: Context): String {
        val jsonString: String
        try {
            jsonString = context.assets.open(FILENAME).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return String.Empty
        }
        return jsonString
    }

    companion object {
        private const val FILENAME = "networks.json"
    }
}