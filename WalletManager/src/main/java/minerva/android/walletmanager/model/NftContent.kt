package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class NftContent(
    val imageUri: String = String.Empty,
    val contentType: ContentType = ContentType.IMAGE,
    val animationUri: String = String.Empty
)