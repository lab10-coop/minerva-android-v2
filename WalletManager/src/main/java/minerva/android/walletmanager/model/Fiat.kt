package minerva.android.walletmanager.model

import minerva.android.apiProvider.model.Price
import kotlin.reflect.full.memberProperties

object Fiat {
    val all: List<String> by lazy {
        mutableListOf<String>().apply {
            Price::class.memberProperties.forEach {
                add(it.name.toUpperCase())
            }
        }
    }
}