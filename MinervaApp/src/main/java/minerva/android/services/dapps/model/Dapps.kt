package minerva.android.services.dapps.model

import minerva.android.R
import minerva.android.kotlinUtils.Empty

object Dapps {

    val valuesOrdered = listOf(
        Dapps.DEHIVE,
        Dapps.STAKEWISE,
        Dapps.HONEYSWAP,
        Dapps.SUPERFLUID,
        Dapps.EPORIO,
        Dapps.QUICKSWAP,
        Dapps.AGAVE,
        Dapps.AAVE,
        Dapps.MINERVA,
        Dapps.SYMMETRIC,
        Dapps.SYMMETRIC_POOLS,
        Dapps.SWAPR,
        Dapps.ZEROALPHA,
        Dapps.ONEINCH,
        Dapps.COWSWAP,
        Dapps.UNISWAP,
        Dapps.PANCAKESWAP,
        Dapps.OPENSEA,
        Dapps.TORNADO_CASH,
        Dapps.CURVE_ETHEREUM,
        Dapps.CURVE_POLYGON,
        Dapps.CURVE_GNOSIS
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
        val DEHIVE = Dapp(
            ShortNames.DEHIVE,
            LongNames.DEHIVE,
            Descriptions.DEHIVE,
            Colors.DEHIVE,
            String.Empty,
            Icons.DEHIVE,
            OpenDappDialogs.Data.DEHIVE
        )
        val SWAPR = Dapp(
            ShortNames.SWAPR,
            LongNames.SWAPR,
            Descriptions.SWAPR,
            Colors.SWAPR,
            String.Empty,
            Icons.SWAPR,
            OpenDappDialogs.Data.SWAPR
        )
        val AGAVE = Dapp(
            ShortNames.AGAVE,
            LongNames.AGAVE,
            Descriptions.AGAVE,
            Colors.AGAVE,
            String.Empty,
            Icons.AGAVE,
            OpenDappDialogs.Data.AGAVE
        )
        val COWSWAP = Dapp(
            ShortNames.COWSWAP,
            LongNames.COWSWAP,
            Descriptions.COWSWAP,
            Colors.COWSWAP,
            String.Empty,
            Icons.COWSWAP,
            OpenDappDialogs.Data.COWSWAP
        )
        val CURVE_ETHEREUM = Dapp(
            ShortNames.CURVE_ETHEREUM,
            LongNames.CURVE_ETHEREUM,
            Descriptions.CURVE_ETHEREUM,
            Colors.CURVE_ETHEREUM,
            String.Empty,
            Icons.CURVE_ETHEREUM,
            OpenDappDialogs.Data.CURVE_ETHEREUM
        )
        val CURVE_POLYGON = Dapp(
            ShortNames.CURVE_POLYGON,
            LongNames.CURVE_POLYGON,
            Descriptions.CURVE_POLYGON,
            Colors.CURVE_POLYGON,
            String.Empty,
            Icons.CURVE_POLYGON,
            OpenDappDialogs.Data.CURVE_POLYGON
        )
        val CURVE_GNOSIS = Dapp(
            ShortNames.CURVE_GNOSIS,
            LongNames.CURVE_GNOSIS,
            Descriptions.CURVE_GNOSIS,
            Colors.CURVE_GNOSIS,
            String.Empty,
            Icons.CURVE_GNOSIS,
            OpenDappDialogs.Data.CURVE_GNOSIS
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
        const val DEHIVE = "DeHive DeFi Protocol"
        const val SWAPR = "Swapr DEX"
        const val AGAVE = "Agave"
        const val COWSWAP = "CowSwap DEX Aggregator"
        const val CURVE_ETHEREUM = "Curve DEX"
        const val CURVE_POLYGON = "Curve DEX"
        const val CURVE_GNOSIS = "Curve DEX"
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
        const val DEHIVE = "DeHive"
        const val SWAPR = "Swapr"
        const val AGAVE = "Agave"
        const val COWSWAP = "CowSwap"
        const val CURVE_ETHEREUM = "Curve (Ethereum)"
        const val CURVE_POLYGON = "Curve (Polygon)"
        const val CURVE_GNOSIS = "Curve (Gnosis Chain)"
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
        const val DEHIVE = "Well-Balanced Crypto Indexes"
        const val SWAPR = "Decentralized Exchange"
        const val AGAVE = "Borrowing / Lending Protocol"
        const val COWSWAP = "DEX Aggregator Service"
        const val CURVE_ETHEREUM = "Decentralized Exchange"
        const val CURVE_POLYGON = "Decentralized Exchange"
        const val CURVE_GNOSIS = "Decentralized Exchange"
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
        const val DEHIVE = "#3F3F4B"
        const val SWAPR = "#3F3F4B"
        const val AGAVE = "#204C44"
        const val COWSWAP = "#7FADBE"
        const val CURVE_ETHEREUM = "#A5A4CB"
        const val CURVE_POLYGON = "#A5A4CB"
        const val CURVE_GNOSIS = "#A5A4CB"
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
        val DEHIVE = R.drawable.ic_dehive
        val SWAPR = R.drawable.ic_swapr
        val AGAVE = R.drawable.ic_agave
        val COWSWAP = R.drawable.ic_cowswap
        val CURVE_ETHEREUM = R.drawable.ic_curve
        val CURVE_POLYGON = R.drawable.ic_curve
        val CURVE_GNOSIS = R.drawable.ic_curve
    }
}