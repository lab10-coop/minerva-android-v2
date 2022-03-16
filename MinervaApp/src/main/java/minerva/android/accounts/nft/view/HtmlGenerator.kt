package minerva.android.accounts.nft.view

import android.util.Base64
import minerva.android.kotlinUtils.Empty
import minerva.android.utils.ensureHexColorPrefix
import minerva.android.walletmanager.model.ContentType
import minerva.android.walletmanager.model.NftContent

object HtmlGenerator {

    fun getNftContentEncodedHtmlFromUrl(nftContent: NftContent): String =
        when (nftContent.contentType) {
            ContentType.VIDEO -> getUnencodedHtmlForVideo(nftContent)
            ContentType.IMAGE -> getUnencodedHtmlForImage(nftContent.imageUri, ensureHexColorPrefix(nftContent.background))
            ContentType.ENCODED_IMAGE -> getUnencodedHtmlForEncodedImage(nftContent.imageUri)
            else -> getInvalidHtml()
        }.let { unencodedHtml ->
            return Base64.encodeToString(unencodedHtml.toByteArray(), Base64.NO_PADDING)
        }

    private fun getInvalidHtml() = String.Empty

    private fun getUnencodedHtmlForEncodedImage(content: String) = "<html lang=\"en\">\n<head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n" +
            "    <style>\n" +
            "      body {\n" +
            "        margin: 0px;\n" +
            "        padding: 0px;\n" +
            "      }\n" +
            "      .nftImageContainer {\n" +
            "        height: 100%;\n" +
            "        display: flex;\n" +
            "        justify-content: center;" +
            "        margin: auto;\n" +
            "      }" +
            "      svg {\n" +
            "        position: fixed;\n" +
            "        top:0;\n" +
            "        left:0;\n" +
            "        height:100%;\n" +
            "        width:100%;\n" +
            "      }\n" +
            "    </style>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <div class=\"nftImageContainer\">\n" +
            "    $content\n" +
            "    </div>\n" +
            "  </body>\n" +
            "</html>"

    private fun getUnencodedHtmlForImage(contentUrl: String, backgroundColor: String) = "<html lang=\"en\">\n<head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n" +
            "    <style>\n" +
            "      body {\n" +
            "        margin: 0px;\n" +
            "        padding: 0px;\n" +
            "        background-color: $backgroundColor;\n" +
            "      }\n" +
            "      .nftImageContainer {\n" +
            "        display: block;\n" +
            "        width: 100%;\n" +
            "        height: 100%;\n" +
            "        background-position: center;\n" +
            "        background-size: contain;\n" +
            "        background-repeat: no-repeat;\n" +
            "      }\n" +
            "    </style>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <div class=\"nftImageContainer\" style=\"background-image: url($contentUrl)\"></div>\n" +
            "  </body>\n" +
            "</html>"

    private fun getUnencodedHtmlForVideo(nftContent: NftContent) = "<html lang=\"en\">\n" +
            "  <head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n" +
            "    <style> body { margin: 0px; padding: 0px; } video.nftVideo { width: 100%; height: auto; min-width: 100%; min-height: 100%; } </style>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <video class=\"nftVideo\" poster=\"${nftContent.imageUri}\" muted controls=\"false\" controlslist=\"nodownload\" loop=\"true\" preload=\"metadata\" style=\"border-radius: initial;\">\n" +
            "      <source src=\"${nftContent.animationUri}\" >\n" +
            "    </video>\n" +
            "  </body>\n" +
            "</html>"
}
