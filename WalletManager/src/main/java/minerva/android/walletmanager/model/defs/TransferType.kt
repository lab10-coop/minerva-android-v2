package minerva.android.walletmanager.model.defs

enum class TransferType {
    TOKEN_TRANSFER,
    COIN_TRANSFER,
    TOKEN_SWAP_APPROVAL,
    TOKEN_SWAP,
    COIN_SWAP,
    SAFE_ACCOUNT_COIN_TRANSFER,
    SAFE_ACCOUNT_TOKEN_TRANSFER,
    DEFAULT_COIN_TX,
    DEFAULT_TOKEN_TX,
    UNKNOWN
}