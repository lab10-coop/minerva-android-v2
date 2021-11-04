package minerva.android.services.dapps.model

import minerva.android.services.dapps.dialog.OpenDappDialog

object OpenDappDialogs {

    object Data {
        val MINERVA =
            OpenDappDialog.Data(Titles.MINERVA, Urls.MINERVA, Instructions.MINERVA)
        val HONEYSWAP =
            OpenDappDialog.Data(Titles.HONEYSWAP, Urls.HONEYSWAP, Instructions.HONEYSWAP)
        val SUPERFLUID =
            OpenDappDialog.Data(Titles.SUPERFLUID, Urls.SUPERFLUID, Instructions.SUPERFLUID)
        val ZEROALPHA =
            OpenDappDialog.Data(Titles.ZEROALPHA, Urls.ZEROALPHA, Instructions.ZEROALPHA)
        val QUICKSWAP =
            OpenDappDialog.Data(Titles.QUICKSWAP, Urls.QUICKSWAP, Instructions.QUICKSWAP)
        val UNISWAP =
            OpenDappDialog.Data(Titles.UNISWAP, Urls.UNISWAP, Instructions.UNISWAP)
        val PANCAKESWAP =
            OpenDappDialog.Data(Titles.PANCAKESWAP, Urls.PANCAKESWAP, Instructions.PANCAKESWAP)
        val TORNADO_CASH =
            OpenDappDialog.Data(Titles.TORNADO_CASH, Urls.TORNADO_CASH, Instructions.TORNADO_CASH)
        val AAVE =
            OpenDappDialog.Data(Titles.AAVE, Urls.AAVE, Instructions.AAVE)
        val STAKEWISE =
            OpenDappDialog.Data(Titles.STAKEWISE, Urls.STAKEWISE, Instructions.STAKEWISE)
        val OPENSEA =
            OpenDappDialog.Data(Titles.OPENSEA, Urls.OPENSEA, Instructions.OPENSEA)
        val ONEINCH =
            OpenDappDialog.Data(Titles.ONEINCH, Urls.ONEINCH, Instructions.ONEINCH)
        val SYMMETRIC_POOLS =
            OpenDappDialog.Data(Titles.SYMMETRIC_POOLS, Urls.SYMMETRIC_POOLS, Instructions.SYMMETRIC_POOLS)
        val SYMMETRIC =
            OpenDappDialog.Data(Titles.SYMMETRIC, Urls.SYMMETRIC, Instructions.SYMMETRIC)
        val EPORIO =
            OpenDappDialog.Data(Titles.EPORIO, Urls.EPORIO, Instructions.EPORIO)
    }

    private object Titles {
        const val MINERVA = Dapps.LongNames.MINERVA
        const val HONEYSWAP = Dapps.LongNames.HONEYSWAP
        const val SUPERFLUID = Dapps.LongNames.SUPERFLUID
        const val ZEROALPHA = Dapps.LongNames.ZEROALPHA
        const val QUICKSWAP = Dapps.LongNames.QUICKSWAP
        const val UNISWAP = Dapps.LongNames.UNISWAP
        const val PANCAKESWAP = Dapps.LongNames.PANCAKESWAP
        const val TORNADO_CASH = Dapps.LongNames.TORNADO_CASH
        const val AAVE = Dapps.LongNames.AAVE
        const val STAKEWISE = Dapps.LongNames.STAKEWISE
        const val OPENSEA = Dapps.LongNames.OPENSEA
        const val ONEINCH = Dapps.LongNames.ONEINCH
        const val SYMMETRIC_POOLS = Dapps.LongNames.SYMMETRIC_POOLS
        const val SYMMETRIC = Dapps.LongNames.SYMMETRIC
        const val EPORIO = Dapps.LongNames.EPORIO
    }

    private object Instructions {
        const val DEFAULT =
            "To connect this DApp with your Minerva Wallet, hit “Open” and look out for “WalletConnect” or the “WalletConnect”-Icon."
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
        const val OPENSEA = DEFAULT
        const val ONEINCH = DEFAULT
        const val SYMMETRIC_POOLS = DEFAULT
        const val SYMMETRIC = DEFAULT
        const val EPORIO = DEFAULT
    }

    private object Urls {
        const val MINERVA = "https://farm.minerva.digital/#connect"
        const val HONEYSWAP = "https://app.honeyswap.org/"
        const val SUPERFLUID = "https://app.superfluid.finance/dashboard"
        const val ZEROALPHA = "https://zeroalpha.art"
        const val QUICKSWAP = "https://quickswap.exchange/"
        const val UNISWAP = "https://app.uniswap.org/"
        const val PANCAKESWAP = "https://pancakeswap.finance"
        const val TORNADO_CASH = "https://app.tornado.cash"
        const val AAVE = "https://app.aave.com/markets"
        const val STAKEWISE = "https://stakewise.io/app/pool"
        const val OPENSEA = "https://opensea.io/"
        const val ONEINCH = "https://app.1inch.io/"
        const val SYMMETRIC_POOLS = "https://xdai-pools.symmetric.exchange/"
        const val SYMMETRIC = "https://xdai.symmetric.exchange/"
        const val EPORIO = "https://epor.io/connect/"
    }
}