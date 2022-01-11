package minerva.android.services.dapps.model

data class DappsWithCategories(
    val sponsored: List<Dapp>,
    val remaining: List<Dapp>
) {
    val isSponsoredVisible: Boolean get() = sponsored.isNotEmpty()
}
