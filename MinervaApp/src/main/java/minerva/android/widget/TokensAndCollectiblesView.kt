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
import minerva.android.databinding.TokensAndCollectiblesLayoutBinding
import minerva.android.extension.toggleVisibleOrGone
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.NO_PADDING
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCTokensList
import minerva.android.widget.token.TokenView
import java.math.BigDecimal

class TokensAndCollectiblesView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var binding =
        TokensAndCollectiblesLayoutBinding.bind(inflate(context, R.layout.tokens_and_collectibles_layout, this))
    private lateinit var callback: TokenView.TokenViewCallback
    private lateinit var parent: ViewGroup
    private var showMainToken = false

    init {
        initView()
        prepareListeners()
    }

    fun prepareView(
        viewGroup: ViewGroup,
        callback: TokenView.TokenViewCallback,
        isOpen: Boolean
    ) {
        parent = viewGroup
        this.callback = callback
        visibleOrGone(isOpen)
    }

    fun prepareTokenLists(account: Account, fiatSymbol: String, tokens: ERCTokensList, isWidgetOpen: Boolean) {
        initMainToken(account, fiatSymbol, callback)
        initTokensList(account, fiatSymbol, tokens.getERC20Tokens(), isWidgetOpen)
        prepareSeparator(tokens.isERC20TokensListNotEmpty() && tokens.isCollectiblesListNotEmpty())
        initCollectiblesList(tokens.getCollectiblesWithBalance(account), isWidgetOpen)
    }

    private fun initView() {
        setPadding(
            Int.NO_PADDING,
            resources.getDimension(R.dimen.margin_xxsmall).toInt(),
            resources.getDimension(R.dimen.margin_normal).toInt(),
            resources.getDimension(R.dimen.margin_xxsmall).toInt()
        )
        orientation = VERTICAL
        isClickable = true
        isFocusable = true
    }


    private fun initTokensList(account: Account, fiatSymbol: String, tokens: List<AccountToken>, isWidgetOpen: Boolean) {
        binding.apply {
            if (isWidgetOpen && tokensContainer.isVisible) {
                tokensContainer.children.forEach { view -> (view as TokenView).endStreamAnimation() }
                tokensContainer.removeAllViews()
                tokens.isNotEmpty().let { areTokensVisible ->
                    tokensHeader.visibleOrGone(areTokensVisible)
                    tokensContainer.visibleOrGone(areTokensVisible)
                    tokens.forEach { accountToken ->
                        tokensContainer.addView(TokenView(context).apply {
                            initView(account, callback, fiatSymbol, accountToken)
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
        collectibles: List<Pair<AccountToken, BigDecimal>>,
        isWidgetOpen: Boolean
    ) {
        binding.apply {
            if (isWidgetOpen && collectiblesContainer.isVisible) {
                with(collectiblesContainer) {
                    collectibles.isNotEmpty().let { visibility ->
                        visibleOrGone(visibility)
                        removeAllViews()
                        collectiblesHeader.visibleOrGone(visibility)
                        collectibles.forEach { collectiblesWithBalance ->
                            addView(
                                CollectibleView(
                                    context,
                                    collectiblesWithBalance.first.token,
                                    collectiblesWithBalance.second
                                )
                            )
                        }
                    }
                    setHeaderArrow(collectiblesHeader, this)
                }
            }
        }
    }

    companion object {
        private const val LEVEL = "level"
        private const val START_DRAWABLE_INDEX = 0
        private const val START_ROTATION_LEVEL = 10000
        private const val STOP_ROTATION_LEVEL = 0
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