package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.transition.TransitionManager
import minerva.android.R
import minerva.android.accounts.nft.view.NftCollectionFragment
import minerva.android.databinding.TokensAndCollectiblesLayoutBinding
import minerva.android.extension.toggleVisibleOrGone
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.NO_PADDING
import minerva.android.kotlinUtils.list.toJsonArray
import minerva.android.main.MainActivity.Companion.ZERO
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.ERCTokensList
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.widget.token.TokenView
import java.math.BigDecimal

class TokensAndCollectiblesView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var binding =
        TokensAndCollectiblesLayoutBinding.bind(inflate(context, R.layout.tokens_and_collectibles_layout, this))
    private lateinit var tokenViewCallback: TokenView.TokenViewCallback
    private lateinit var collectibleViewCallback: CollectibleView.CollectibleViewCallback
    private lateinit var parent: ViewGroup
    private var showMainToken = false

    init {
        initView()
        prepareListeners()
    }

    fun prepareView(
        viewGroup: ViewGroup,
        tokenViewCallback: TokenView.TokenViewCallback,
        collectibleViewCallback: CollectibleView.CollectibleViewCallback,
        isOpen: Boolean
    ) {
        parent = viewGroup
        this.tokenViewCallback = tokenViewCallback
        this.collectibleViewCallback = collectibleViewCallback
        visibleOrGone(isOpen)
    }

    fun prepareTokenLists(account: Account, fiatSymbol: String, tokens: ERCTokensList, isWidgetOpen: Boolean) {
        initMainToken(account, fiatSymbol, tokenViewCallback)
        initTokensList(account, fiatSymbol, tokens.getERC20Tokens(), isWidgetOpen)
        prepareSeparator(tokens.isERC20TokensListNotEmpty() && tokens.isCollectiblesListNotEmpty())
        initCollectiblesList(account, tokens.getCollectionsWithBalance(account), isWidgetOpen)
    }

    private fun initView() {
        setPadding(
            Int.NO_PADDING,
            resources.getDimension(R.dimen.margin_xxsmall).toInt(),
            Int.NO_PADDING,
            resources.getDimension(R.dimen.margin_xxsmall).toInt()
        )
        orientation = VERTICAL
        isClickable = true
        isFocusable = true
    }


    private fun initTokensList(account: Account, fiatSymbol: String, tokens: List<AccountToken>, isWidgetOpen: Boolean) {
        binding.apply {
            tokensContainer.children.forEach { view -> (view as TokenView).endStreamAnimation() }
            tokens.isNotEmpty().let { areTokensVisible ->
                tokensHeader.visibleOrGone(areTokensVisible)
                tokensContainer.visibleOrGone(areTokensVisible)
                if (isWidgetOpen && tokensContainer.isVisible) {
                    tokensContainer.removeAllViews()
                    tokens.forEach { accountToken ->
                        tokensContainer.addView(TokenView(context).apply {
                            initView(account, tokenViewCallback, fiatSymbol, accountToken)
                            resources.getDimensionPixelOffset(R.dimen.margin_xxsmall)
                                .let { padding -> updatePadding(Int.NO_PADDING, padding, Int.NO_PADDING, padding) }
                        })
                    }
                }
                setHeaderArrow(tokensHeader, tokensContainer)
            }
        }
    }

    fun endStreamAnimations() {
        binding.tokensContainer.children.forEach { view ->
            (view as TokenView).endStreamAnimation()
        }
    }

    private fun prepareListeners() {
        binding.apply {
            setOnHeaderClickListener(tokensHeader, tokensContainer)
            setOnHeaderClickListener(collectiblesHeader, collectiblesContainer)
        }
    }

    private fun setOnHeaderClickListener(header: TextView, container: LinearLayout) {
        header.setOnClickListener {
            TransitionManager.beginDelayedTransition(parent)
            container.toggleVisibleOrGone()
            setHeaderArrow(header, container)
        }
    }

    private fun setHeaderArrow(header: TextView, container: LinearLayout) {
        val arrowRes = if (container.isVisible) {
            R.drawable.ic_arrow_up
        } else {
            R.drawable.ic_arrow_down
        }
        header.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(context, arrowRes), null, null, null)
    }

    private fun getAnimationLevels(isContainerVisible: Boolean) =
        if (isContainerVisible) Pair(START_ROTATION_LEVEL, STOP_ROTATION_LEVEL)
        else Pair(STOP_ROTATION_LEVEL, START_ROTATION_LEVEL)

    private fun prepareSeparator(isSeparatorVisible: Boolean) = with(binding) {
        collectiblesSeparator.visibleOrGone(isSeparatorVisible)
    }

    private fun initCollectiblesList(
        account: Account,
        collectibles: List<Pair<AccountToken, BigDecimal>>,
        isWidgetOpen: Boolean
    ) {
        binding.apply {
            with(collectiblesContainer) {
                collectibles.isNotEmpty().let { visibility ->
                    visibleOrGone(visibility)
                    collectiblesHeader.visibleOrGone(visibility)
                    if (isWidgetOpen && collectiblesContainer.isVisible) {
                        removeAllViews()
                        //list for favorites tokens
                        val favoriteTokens: MutableList<ERCToken> = mutableListOf()
                        //filling favoriteTokens list from main account
                        account.accountTokens.forEach { accountToken ->
                            val token: ERCToken = accountToken.token
                            if (token.isFavorite) {
                                //check token list already has this token
                                val isTokenInList: ERCToken? = favoriteTokens.find { it.tokenId == token.tokenId }
                                if (null == isTokenInList)
                                    favoriteTokens.add(token)
                            }
                        }
                        //create "My Favorites" group(with nft group) like usual item
                        if (favoriteTokens.size > ZERO) {
                            //create json array of tokens addresses for send/show it in "NftCollectionFragment"
                            val favoriteTokenAddresses: MutableList<String> = mutableListOf()
                            favoriteTokens.forEach { token ->
                                if (!favoriteTokenAddresses.contains(token.address))
                                    favoriteTokenAddresses.add(token.address)
                            }
                            //addresses to json array - for transfer it through ERCToken wrapper
                            val favoriteTokenAddressesToJson: String = favoriteTokenAddresses.toJsonArray()
                            //add "My Favorites" group to token/nft list like usual token/nft item
                            addView(CollectibleView(context).apply {
                                initView(
                                    account,
                                    collectibleViewCallback,
                                    //create mock token entity with favorites tokens data(for recognize it latter like group item case)
                                    ERCToken(
                                        chainId = FAVORITE_GROUP_ID,
                                        symbol = resources.getString(R.string.my_favorites),
                                        address = favoriteTokenAddressesToJson,
                                        collectionName = resources.getString(R.string.my_favorites_item_description),
                                        type = TokenType.ERC1155,
                                        logoURI = NftCollectionFragment.favoriteLogoUrl),
                                    favoriteTokens.size.toBigDecimal(), //count of token/nft in items
                                    isGroup = true
                                )
                            })
                        }

                        collectibles.forEach { collectiblesWithBalance ->
                            addView(CollectibleView(context).apply {
                                initView(
                                    account,
                                    collectibleViewCallback,
                                    collectiblesWithBalance.first.token,
                                    collectiblesWithBalance.second
                                )
                            })
                        }
                    }
                }
                setHeaderArrow(collectiblesHeader, this)
            }
        }
    }

    companion object {
        private const val LEVEL = "level"
        private const val START_DRAWABLE_INDEX = 0
        private const val START_ROTATION_LEVEL = 10000
        private const val STOP_ROTATION_LEVEL = 0
        const val FAVORITE_GROUP_ID = -2
    }

    //TODO this method is not used, because Asset Manage screen is not implemented yet - ready to use UI
    private fun initMainToken(account: Account, fiatSymbol: String, callback: TokenView.TokenViewCallback) {
        binding.apply {
            if (showMainToken) {
                tokensSeparator.visible()
                with(mainToken) {
                    visible()
                    initView(account, callback, fiatSymbol)
                }
            }
        }
    }
}