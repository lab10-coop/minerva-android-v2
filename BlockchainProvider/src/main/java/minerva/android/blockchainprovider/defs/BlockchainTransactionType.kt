package minerva.android.blockchainprovider.defs

enum class BlockchainTransactionType {
    COIN_TRANSFER,
    COIN_SWAP,
    TOKEN_TRANSFER,
    TOKEN_SWAP_APPROVAL,
    TOKEN_SWAP,
    SAFE_ACCOUNT_COIN_TRANSFER,
    SAFE_ACCOUNT_TOKEN_TRANSFER,
    DEFAULT_COIN_TX,
    DEFAULT_TOKEN_TX,
    ERC721_TRANSFER,
    ERC1155_TRANSFER,
    UNKNOWN
}