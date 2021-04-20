package minerva.android.walletmanager.model

import minerva.android.apiProvider.model.FiatPrice
import kotlin.reflect.full.memberProperties

object Fiat {
    val all: List<String> by lazy {
        mutableListOf<String>().apply {
            FiatPrice::class.memberProperties.forEach {
                add(it.name.toUpperCase())
            }
        }
    }
}