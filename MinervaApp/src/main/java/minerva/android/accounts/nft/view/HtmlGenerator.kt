package minerva.android.accounts.nft.view

import android.util.Base64

object HtmlGenerator {

    private const val MP4_TYPE = ".mp4"

    fun getNftContentEncodedHtmlFromUrl(url: String): String {
        val unencodedHtml = if (url.contains(MP4_TYPE)) {
            getUnencodedHtmlForVideo(url)
        } else {
            getUnencodedHtmlForImage(url)
        }
        return Base64.encodeToString(unencodedHtml.toByteArray(), Base64.NO_PADDING)
    }

    private fun getUnencodedHtmlForImage(contentUrl: String) = "<html lang=\"en\">\n<head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n" +
            "    <style>\n" +
            "      body {\n" +
            "        margin: 0px;\n" +
            "        padding: 0px;\n" +
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

    private fun getUnencodedHtmlForVideo(contentUrl: String) = "<html lang=\"en\">\n" +
            "  <head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n" +
            "    <style> body { margin: 0px; padding: 0px; } video.nftVideo { width: 100%; height: auto; } </style>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <video class=\"nftVideo\" autoplay muted controls=\"false\" controlslist=\"nodownload\" loop=\"true\" preload=\"metadata\" style=\"border-radius: initial;\">\n" +
            "      <source src=\"$contentUrl\" type=\"video/mp4\">\n" +
            "    </video>\n" +
            "  </body>\n" +
            "</html>"

}
