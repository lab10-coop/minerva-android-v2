package minerva.android.services.dapps.model

import minerva.android.R
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyResource
import minerva.android.services.dapps.dialog.OpenDappDialog
import kotlin.reflect.full.memberProperties

object Dapps {
    val values = listOf(
        Dapp(
            Labels.MINERVA,
            Backgrounds.MINERVA,
            OpenDappDialog.Data(
                Titles.MINERVA,
                Urls.MINERVA,
                Descriptions.MINERVA,
                Confirms.MINERVA
            )
        ),
        Dapp(
            Labels.ZEROALPHA,
            Backgrounds.ZEROALPHA,
            OpenDappDialog.Data(
                Titles.ZEROALPHA,
                Urls.ZEROALPHA,
                Descriptions.ZEROALPHA,
                Confirms.ZEROALPHA
            )
        ),
        Dapp(
            Labels.SUPERFLUID,
            Backgrounds.SUPERFLUID,
            OpenDappDialog.Data(
                Titles.SUPERFLUID,
                Urls.SUPERFLUID,
                Descriptions.SUPERFLUID,
                Confirms.SUPERFLUID
            )
        ),
        Dapp(
            Labels.HONEYSWAP,
            Backgrounds.HONEYSWAP,
            OpenDappDialog.Data(
                Titles.HONEYSWAP,
                Urls.HONEYSWAP,
                Descriptions.HONEYSWAP,
                Confirms.HONEYSWAP
            )
        ),

        Dapp(
            Labels.QUICKSWAP,
            Backgrounds.QUICKSWAP,
            OpenDappDialog.Data(
                Titles.QUICKSWAP,
                Urls.QUICKSWAP,
                Descriptions.QUICKSWAP,
                Confirms.QUICKSWAP
            )
        ),

        Dapp(
            Labels.UNISWAP,
            Backgrounds.UNISWAP,
            OpenDappDialog.Data(
                Titles.UNISWAP,
                Urls.UNISWAP,
                Descriptions.UNISWAP,
                Confirms.UNISWAP
            )
        ),
        Dapp(
            Labels.PANCAKESWAP,
            Backgrounds.PANCAKESWAP,
            OpenDappDialog.Data(
                Titles.PANCAKESWAP,
                Urls.PANCAKESWAP,
                Descriptions.PANCAKESWAP,
                Confirms.PANCAKESWAP
            )
        ),
        Dapp(
            Labels.TORNADO_CASH,
            Backgrounds.TORNADO_CASH,
            OpenDappDialog.Data(
                Titles.TORNADO_CASH,
                Urls.TORNADO_CASH,
                Descriptions.TORNADO_CASH,
                Confirms.TORNADO_CASH
            )
        ),

        Dapp(
            Labels.AAVE,
            Backgrounds.AAVE,
            OpenDappDialog.Data(
                Titles.AAVE,
                Urls.AAVE,
                Descriptions.AAVE,
                Confirms.AAVE
            )
        ),
        Dapp(
            Labels.STAKEWISE,
            Backgrounds.STAKEWISE,
            OpenDappDialog.Data(
                Titles.STAKEWISE,
                Urls.STAKEWISE,
                Descriptions.STAKEWISE,
                Confirms.STAKEWISE
            )
        )
    )

    private object Labels {
        const val MINERVA = "Minerva"
        const val HONEYSWAP = "Honeyswap"
        const val SUPERFLUID = "Superfluid"
        const val ZEROALPHA = "ZeroAlpha"
        const val QUICKSWAP = "Quickswap"
        const val UNISWAP = "Uniswap"
        const val PANCAKESWAP = "Pancakeswap"
        const val TORNADO_CASH = "Tornado Cash"
        const val AAVE = "AAVE"
        const val STAKEWISE = "Stakewise"
    }

    private object Urls {
        const val MINERVA = "https://farm.minerva.digital"
        const val HONEYSWAP = "https://app.honeyswap.org/"
        const val SUPERFLUID = "https://app.superfluid.finance/dashboard"
        const val ZEROALPHA = "https://zeroalpha.art"
        const val QUICKSWAP = "https://quickswap.exchange/#/swap"
        const val UNISWAP = "https://app.uniswap.org/#/swap"
        const val PANCAKESWAP = "https://pancakeswap.finance"
        const val TORNADO_CASH = "https://tornado.cash"
        const val AAVE = "https://aave.com"
        const val STAKEWISE = "https://stakewise.io"
    }

    // TODO MNR-622 Replace all missing titles before releasing to production. Titles are to be provided by client.
    private object Titles {
        const val MINERVA = "Minerva Streaming Farms"
        const val HONEYSWAP = Labels.HONEYSWAP
        const val SUPERFLUID = Labels.SUPERFLUID
        const val ZEROALPHA = Labels.ZEROALPHA
        const val QUICKSWAP = Labels.QUICKSWAP
        const val UNISWAP = Labels.UNISWAP
        const val PANCAKESWAP = Labels.PANCAKESWAP
        const val TORNADO_CASH = Labels.TORNADO_CASH
        const val AAVE = Labels.AAVE
        const val STAKEWISE = Labels.STAKEWISE
    }

    // TODO MNR-622 Replace all missing descriptions before releasing to production. Descriptions are to be provided by client.
    private object Descriptions {
        const val DEFAULT = R.string.default_dapp_dialog_description
        const val MINERVA = DEFAULT
        const val HONEYSWAP = DEFAULT
        const val SUPERFLUID = DEFAULT
        const val ZEROALPHA = DEFAULT
        const val QUICKSWAP = DEFAULT
        const val UNISWAP = DEFAULT
        const val PANCAKESWAP = DEFAULT
        const val TORNADO_CASH = DEFAULT
        const val AAVE = DEFAULT
        const val STAKEWISE = DEFAULT
    }

    // TODO MNR-622 Replace all missing confirmation texts before releasing to production. Texts are to be provided by client.
    private object Confirms {
        const val DEFAULT = R.string.default_dapp_dialog_confirm
        const val MINERVA = DEFAULT
        const val HONEYSWAP = DEFAULT
        const val SUPERFLUID = DEFAULT
        const val ZEROALPHA = DEFAULT
        const val QUICKSWAP = DEFAULT
        const val UNISWAP = DEFAULT
        const val PANCAKESWAP = DEFAULT
        const val TORNADO_CASH = DEFAULT
        const val AAVE = DEFAULT
        const val STAKEWISE = DEFAULT
    }

    // TODO MNR-622 Confirm that defined backgrounds are matching client definition
    private object Backgrounds {
        const val DEFAULT = R.drawable.dapp_background_default
        const val MINERVA = R.drawable.dapp_background_minerva
        const val HONEYSWAP = R.drawable.dapp_background_honeyswap
        const val SUPERFLUID = DEFAULT
        const val ZEROALPHA = DEFAULT
        const val QUICKSWAP = DEFAULT
        const val UNISWAP = DEFAULT
        const val PANCAKESWAP = DEFAULT
        const val TORNADO_CASH = DEFAULT
        const val AAVE = DEFAULT
        const val STAKEWISE = DEFAULT
    }
}