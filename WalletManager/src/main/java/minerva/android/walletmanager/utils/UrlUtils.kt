package minerva.android.walletmanager.utils

fun parseIPFSContentUrl(url: String): String {
    return when {
        url.startsWith(EXTENDED_IPFS_PREFIX) -> {
            url.replaceFirst(EXTENDED_IPFS_PREFIX, HTTPS_FOR_IPFS_PREFIX)
        }
        url.startsWith(IPFS_PREFIX) -> {
            url.replaceFirst(IPFS_PREFIX, HTTPS_FOR_IPFS_PREFIX)
        }
        else -> url
    }
}

private const val IPFS_PREFIX = "ipfs://"
private const val EXTENDED_IPFS_PREFIX = "ipfs://ipfs/"
private const val HTTPS_FOR_IPFS_PREFIX = "https://ipfs.io/ipfs/"