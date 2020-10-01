package minerva.android.widget.clubCard

interface ClubCardStateCallback {
    fun onLoading(loading: Boolean)
    fun onCardDataPrepared(data: String)
    fun onError()
}