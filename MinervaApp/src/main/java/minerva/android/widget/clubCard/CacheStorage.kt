package minerva.android.widget.clubCard

interface CacheStorage {
    fun save(cardUrl: String?, cardSource: String): Boolean
    fun load(cardUrl: String?): String?
}