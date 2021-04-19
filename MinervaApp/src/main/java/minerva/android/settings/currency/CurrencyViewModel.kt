package minerva.android.settings.currency

import android.util.Log
import minerva.android.R
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.mapper.StringArrayMapper
import minerva.android.walletmanager.model.Currency
import minerva.android.walletmanager.storage.LocalStorage

class CurrencyViewModel(private val localStorage: LocalStorage) : BaseViewModel() {

    fun getCurrentCurrency() {
        val currency = localStorage.loadCurrentCurrency()
        Log.e("klop", "Current currency: $currency")
    }

    fun getCurrencyList() {
        Log.e("klop", "currencies:")
        Currency.currencies.forEach {
            Log.e("klop", "$it")
        }
    }
}