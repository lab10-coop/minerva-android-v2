package minerva.android.widget.clubCard

import android.content.SharedPreferences

class CacheStorageImpl(private val storage: SharedPreferences) : CacheStorage {

    override fun save(cardUrl: String?, cardSource: String) = storage.edit().putString(cardUrl, cardSource).commit()

    override fun load(cardUrl: String?): String? = storage.getString(cardUrl, null)
}