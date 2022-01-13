package minerva.android.services.dapps.model

data class DappsWithCategories(
    val favorite: List<Dapp>,
    val sponsored: List<Dapp>,
    val remaining: List<Dapp>
) {
    val isFavoriteVisible: Boolean get() = favorite.isNotEmpty()
    val isSponsoredVisible: Boolean get() = sponsored.isNotEmpty()
}
