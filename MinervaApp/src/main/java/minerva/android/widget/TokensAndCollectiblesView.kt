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
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Collectible

@SuppressLint("CustomView")
class TokensAndCollectiblesView(
    private val parent: ViewGroup,
    private val account: Account,
    private val callback: TokenView.TokenViewCallback,
    private val showMainToken: Boolean
) : LinearLayout(parent.context) {

    private var binding = TokensAndCollectiblesLayoutBinding.bind(inflate(context, R.layout.tokens_and_collectibles_layout, this))

    init {
        initView()
        prepareListeners()
        initMainToken(account, callback)
        initAssetsList()
        initCollectiblesList()
    }

    private fun initView() {
        orientation = VERTICAL
        setPadding(Int.NO_PADDING, Int.NO_PADDING, resources.getDimension(R.dimen.margin_normal).toInt(), Int.NO_PADDING)
        isClickable = true
        isFocusable = true
    }

    private fun initCollectiblesList() {
        //TODO implement adding views to collectibles container
        //TODO list of Collectibles made only for UI purposes
        val collectibles = listOf(
            Collectible("POAP", "The Proof of Minerva Protocol", 3),
            Collectible("POAP #2", "The Proof of Wilc Protocol", 2)
        )

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

    private fun initAssetsList() {
        binding.apply {
            account.accountAssets.forEachIndexed { index, _ ->
                tokensContainer.removeAllViews()
                tokensContainer.addView(TokenView(context).apply {
                    initView(account, callback, index)
                })
            }
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