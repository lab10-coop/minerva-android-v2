package minerva.android.widget

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.transition.TransitionManager
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import minerva.android.R
import minerva.android.databinding.TokensAndCollectiblesLayoutBinding
import minerva.android.extension.toggleVisibleOrGone
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.NO_PADDING
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.Collectible
import minerva.android.widget.token.TokenView

@SuppressLint("ViewConstructor")
class TokensAndCollectiblesView(
    private val parent: ViewGroup,
    private val account: Account,
    private val callback: TokenView.TokenViewCallback,
    private val showMainToken: Boolean
) : LinearLayout(parent.context) {

    private var binding = TokensAndCollectiblesLayoutBinding.bind(inflate(context, R.layout.tokens_and_collectibles_layout, this))

    init {
        initView()
        initMainToken(account, callback)
        prepareListeners()
        initTokensList()
    }

    private fun initView() {
        orientation = VERTICAL
        setPadding(Int.NO_PADDING, Int.NO_PADDING, resources.getDimension(R.dimen.margin_normal).toInt(), Int.NO_PADDING)
        isClickable = true
        isFocusable = true
    }

    //TODO this method is not used, because Collectiles are not implemented yet - ready to use UI
    private fun initCollectiblesList(collectibles: List<Collectible>) {
        //TODO implement adding views to collectibles container
        //TODO list of Collectibles made only for UI purposes
//        val collectibles = listOf(
//            Collectible("POAP", "The Proof of Minerva Protocol", 3),
//            Collectible("POAP #2", "The Proof of Wilc Protocol", 2)
//        )

        binding.apply {
            collectiblesContainer.apply {
                visible()
                removeAllViews()
                collectiblesSeparator.visibleOrGone(collectibles.isNotEmpty())
                collectiblesHeader.visibleOrGone(collectibles.isNotEmpty())
                collectibles.forEach {
                    addView(CollectibleView(context, it))
                }
            }
        }
    }

    //TODO this method is not used, because Asset Manage screen is not implemented yet - ready to use UI
    private fun initMainToken(account: Account, callback: TokenView.TokenViewCallback) {
        binding.apply {
            if (showMainToken) {
                tokensSeparator.visible()
                with(mainToken) {
                    visible()
                    initView(account, callback)
                }
            }
        }
    }

    private fun initTokensList() {
        binding.apply {
            tokensContainer.removeAllViews()
            account.accountTokens.forEachIndexed { index, _ ->
                    tokensContainer.addView(TokenView(context).apply {
                        initView(account, callback, index)
                    })
                }
        }
    }

    private fun prepareListeners() {
        binding.apply {
            //TODO listeners turned off until Collectibles support implementation
            //setOnHeaderClickListener(tokensHeader, tokensContainer)
            //setOnHeaderClickListener(collectiblesHeader, collectiblesContainer)
        }
    }

    private fun setOnHeaderClickListener(header: TextView, container: LinearLayout) {
        header.setOnClickListener {
            TransitionManager.beginDelayedTransition(parent)
            container.toggleVisibleOrGone()
            getAnimationLevels(container.isVisible).let {
                ObjectAnimator.ofInt(header.compoundDrawables[LEFT_DRAWABLE_INDEX], LEVEL, it.first, it.second).start()
            }
        }
    }

    private fun getAnimationLevels(isContainerVisible: Boolean) =
        if (isContainerVisible) Pair(START_ROTATION_LEVEL, STOP_ROTATION_LEVEL)
        else Pair(STOP_ROTATION_LEVEL, START_ROTATION_LEVEL)

    companion object {
        private const val LEVEL = "level"
        private const val LEFT_DRAWABLE_INDEX = 0
        private const val START_ROTATION_LEVEL = 10000
        private const val STOP_ROTATION_LEVEL = 0
    }
}