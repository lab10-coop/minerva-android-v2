package minerva.android.walletmanager.model.contract

enum class ContractTransactions(val type: String) {
    APPROVE("approve"),
    SWAP_EXACT_TOKENS_FOR_TOKENS("swapExactTokensForTokens"),
    SWAP_EXACT_TOKENS_FOR_ETH("swapExactTokensForETH")
}