package minerva.android.walletmanager.repository.asset

import minerva.android.walletmanager.model.minervaprimitives.account.AssetBalance

interface AssetBalanceRepository {
    var assetBalances: MutableList<AssetBalance>
}