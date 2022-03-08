package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class NftContent(
    var imageUri: String = String.Empty,
    var contentType: ContentType = ContentType.INVALID,
    var animationUri: String = String.Empty,
    var tokenUri: String = String.Empty,
    var background: String = String.Empty
)