package minerva.android.walletmanager.repository.asset

import minerva.android.walletmanager.model.minervaprimitives.account.AssetBalance

class AssetBalanceRepositoryImpl : AssetBalanceRepository {
    override var assetBalances: MutableList<AssetBalance> = mutableListOf()
}