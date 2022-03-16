package minerva.android.configProvider.model.walletConfig

import minerva.android.kotlinUtils.Empty

data class NftContentPayload(
    var imageUri: String = String.Empty,
    var contentType: String = String.Empty,
    var animationUri: String = String.Empty,
    var tokenUri: String = String.Empty,
    var background: String = String.Empty,
    var description: String = String.Empty
)