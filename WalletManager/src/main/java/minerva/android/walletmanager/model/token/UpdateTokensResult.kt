package minerva.android.walletmanager.model.token

data class UpdateTokensResult(
    val shouldSafeNewTokens: Boolean,
    val tokensPerChainIdMap: Map<Int, List<ERCToken>>
)
