package minerva.android.services.dapps.model

import minerva.android.R
import minerva.android.kotlinUtils.Empty

object Dapps {

    val valuesOrdered = listOf(
        Dapps.STAKEWISE,
        Dapps.HONEYSWAP,
        Dapps.SUPERFLUID,
        Dapps.EPORIO,
        Dapps.QUICKSWAP,
        Dapps.AAVE,
        Dapps.MINERVA,
        Dapps.SYMMETRIC,
        Dapps.SYMMETRIC_POOLS,
        Dapps.ZEROALPHA,
        Dapps.ONEINCH,
        Dapps.UNISWAP,
        Dapps.PANCAKESWAP,
        Dapps.OPENSEA,
        Dapps.TORNADO_CASH
    )

    private object Dapps {
        val MINERVA = Dapp(
            ShortNames.MINERVA,
            LongNames.MINERVA,
            Descriptions.MINERVA,
            Colors.MINERVA,
            String.Empty,
            Icons.MINERVA,
            OpenDappDialogs.Data.MINERVA
        )
        val HONEYSWAP = Dapp(
            ShortNames.HONEYSWAP,
            LongNames.HONEYSWAP,
            Descriptions.HONEYSWAP,
            Colors.HONEYSWAP,
            String.Empty,
            Icons.HONEYSWAP,
            OpenDappDialogs.Data.HONEYSWAP
        )
        val SUPERFLUID = Dapp(
            ShortNames.SUPERFLUID,
            LongNames.SUPERFLUID,
            Descriptions.SUPERFLUID,
            Colors.SUPERFLUID,
            String.Empty,
            Icons.SUPERFLUID,
            OpenDappDialogs.Data.SUPERFLUID
        )
        val ZEROALPHA = Dapp(
            ShortNames.ZEROALPHA,
            LongNames.ZEROALPHA,
            Descriptions.ZEROALPHA,
            Colors.ZEROALPHA,
            String.Empty,
            Icons.ZEROALPHA,
            OpenDappDialogs.Data.ZEROALPHA
        )
        val QUICKSWAP = Dapp(
            ShortNames.QUICKSWAP,
            LongNames.QUICKSWAP,
            Descriptions.QUICKSWAP,
            Colors.QUICKSWAP,
            String.Empty,
            Icons.QUICKSWAP,
            OpenDappDialogs.Data.QUICKSWAP
        )
        val UNISWAP = Dapp(
            ShortNames.UNISWAP,
            LongNames.UNISWAP,
            Descriptions.UNISWAP,
            Colors.UNISWAP,
            String.Empty,
            Icons.UNISWAP,
            OpenDappDialogs.Data.UNISWAP
        )
        val PANCAKESWAP = Dapp(
            ShortNames.PANCAKESWAP,
            LongNames.PANCAKESWAP,
            Descriptions.PANCAKESWAP,
            Colors.PANCAKESWAP,
            String.Empty,
            Icons.PANCAKESWAP,
            OpenDappDialogs.Data.PANCAKESWAP
        )
        val TORNADO_CASH = Dapp(
            ShortNames.TORNADO_CASH,
            LongNames.TORNADO_CASH,
            Descriptions.TORNADO_CASH,
            Colors.TORNADO_CASH,
            String.Empty,
            Icons.TORNADO_CASH,
            OpenDappDialogs.Data.TORNADO_CASH
        )
        val AAVE = Dapp(
            ShortNames.AAVE,
            LongNames.AAVE,
            Descriptions.AAVE,
            Colors.AAVE,
            String.Empty,
            Icons.AAVE,
            OpenDappDialogs.Data.AAVE
        )
        val STAKEWISE = Dapp(
            ShortNames.STAKEWISE,
            LongNames.STAKEWISE,
            Descriptions.STAKEWISE,
            Colors.STAKEWISE,
            String.Empty,
            Icons.STAKEWISE,
            OpenDappDialogs.Data.STAKEWISE
        )
        val OPENSEA = Dapp(
            ShortNames.OPENSEA,
            LongNames.OPENSEA,
            Descriptions.OPENSEA,
            Colors.OPENSEA,
            String.Empty,
            Icons.OPENSEA,
            OpenDappDialogs.Data.OPENSEA
        )
        val ONEINCH = Dapp(
            ShortNames.ONEINCH,
            LongNames.ONEINCH,
            Descriptions.ONEINCH,
            Colors.ONEINCH,
            String.Empty,
            Icons.ONEINCH,
            OpenDappDialogs.Data.ONEINCH
        )
        val SYMMETRIC_POOLS = Dapp(
            ShortNames.SYMMETRIC_POOLS,
            LongNames.SYMMETRIC_POOLS,
            Descriptions.SYMMETRIC_POOLS,
            Colors.SYMMETRIC_POOLS,
            String.Empty,
            Icons.SYMMETRIC_POOLS,
            OpenDappDialogs.Data.SYMMETRIC_POOLS
        )
        val SYMMETRIC = Dapp(
            ShortNames.SYMMETRIC,
            LongNames.SYMMETRIC,
            Descriptions.SYMMETRIC,
            Colors.SYMMETRIC,
            String.Empty,
            Icons.SYMMETRIC,
            OpenDappDialogs.Data.SYMMETRIC
        )
        val EPORIO = Dapp(
            ShortNames.EPORIO,
            LongNames.EPORIO,
            Descriptions.EPORIO,
            Colors.EPORIO,
            String.Empty,
            Icons.EPORIO,
            OpenDappDialogs.Data.EPORIO
        )
    }

    object LongNames {
        const val MINERVA = "Minerva Streaming Farms"
        const val HONEYSWAP = "Honeyswap DEX"
        const val SUPERFLUID = "Superfluid Dashboard"
        const val ZEROALPHA = "Zeroalpha NFT Marketplace"
        const val QUICKSWAP = "QuickSwap DEX"
        const val UNISWAP = "Uniswap DEX"
        const val PANCAKESWAP = "PancakeSwap DEX"
        const val TORNADO_CASH = "Tornado Cash"
        const val AAVE = "AAVE"
        const val STAKEWISE = "StakeWise ETH 2.0 Staking"
        const val OPENSEA = "OpenSea NFT Marketplace"
        const val ONEINCH = "1inch DEX Aggregator"
        const val SYMMETRIC_POOLS = "Symmetric DEX Pools"
        const val SYMMETRIC = "Symmetric DEX"
        const val EPORIO = "Eporio NFT Marketplace"
    }

    private object ShortNames {
        const val MINERVA = "Minerva Farms"
        const val HONEYSWAP = "Honeyswap"
        const val SUPERFLUID = "Superfluid"
        const val ZEROALPHA = "ZeroAlpha"
        const val QUICKSWAP = "QuickSwap"
        const val UNISWAP = "Uniswap"
        const val PANCAKESWAP = "PancakeSwap"
        const val TORNADO_CASH = "Tornado Cash"
        const val AAVE = "AAVE"
        const val STAKEWISE = "StakeWise"
        const val OPENSEA = "OpenSea"
        const val ONEINCH = "1inch"
        const val SYMMETRIC_POOLS = "Symmetric Pools"
        const val SYMMETRIC = "Symmetric"
        const val EPORIO = "Eporio"
    }

    private object Descriptions {
        const val MINERVA = "MIVA Liquidity Mining"
        const val HONEYSWAP = "Decentralized Exchange"
        const val SUPERFLUID = "Programmable Cashflows"
        const val ZEROALPHA = "Always-on-Sale Artwork"
        const val QUICKSWAP = "Decentralized Exchange"
        const val UNISWAP = "Decentralized Exchange"
        const val PANCAKESWAP = "Decentralized Exchange"
        const val TORNADO_CASH = "Private Transactions"
        const val AAVE = "Borrowing / Lending Protocol"
        const val STAKEWISE = "ETH 2.0 Staking Service"
        const val OPENSEA = "NFT Marketplace"
        const val ONEINCH = "DEX Aggregator Service"
        const val SYMMETRIC_POOLS = "Decentralized Exchange"
        const val SYMMETRIC = "Decentralized Exchange"
        const val EPORIO = "NFT Marketplace"
    }

    private object Colors {
        const val MINERVA = "#5858ed"
        const val HONEYSWAP = "#ffaa00"
        const val SUPERFLUID = "#908f9d"
        const val ZEROALPHA = "#3f3f4b"
        const val QUICKSWAP = "#418aca"
        const val UNISWAP = "#e8006f"
        const val PANCAKESWAP = "#39c3cc"
        const val TORNADO_CASH = "#5c5d6d"
        const val AAVE = "#cc88bd"
        const val STAKEWISE = "#4b4fab"
        const val OPENSEA = "#1868b7"
        const val ONEINCH = "#192951"
        const val SYMMETRIC_POOLS = "#334956"
        const val SYMMETRIC = "#334956"
        const val EPORIO = "#00b4c7"
    }

    private object Icons {
        val MINERVA = R.drawable.ic_minerva_icon
        val HONEYSWAP = R.drawable.ic_honeyswap
        val SUPERFLUID = R.drawable.ic_superfluid
        val ZEROALPHA = R.drawable.ic_zeroalpha
        val QUICKSWAP = R.drawable.ic_quickswap
        val UNISWAP = R.drawable.ic_uniswap
        val PANCAKESWAP = R.drawable.ic_pancakeswap
        val TORNADO_CASH = R.drawable.ic_tornado_cash
        val AAVE = R.drawable.ic_aave
        val STAKEWISE = R.drawable.ic_stakewise
        val OPENSEA = R.drawable.ic_opensea
        val ONEINCH = R.drawable.ic_oneinch
        val SYMMETRIC_POOLS = R.drawable.ic_symmetric
        val SYMMETRIC = R.drawable.ic_symmetric
        val EPORIO = R.drawable.ic_eporio
    }
}