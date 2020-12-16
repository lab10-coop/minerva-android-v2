package minerva.android.accounts.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.accounts.listener.AccountsAdapterListener
import minerva.android.databinding.AccountListRowBinding
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.widget.TokenView
import minerva.android.widget.TokensAndCollectiblesView
import minerva.android.widget.repository.getNetworkIcon

@SuppressLint("CustomView")
class AccountViewHolder(private val view: View, private val viewGroup: ViewGroup) : TokenView.TokenViewCallback,
    RecyclerView.ViewHolder(view) {

    private var binding = AccountListRowBinding.bind(view)

    private lateinit var listener: AccountsAdapterListener
    private var rawPosition: Int = Int.InvalidIndex

    private val isOpen
        get() = binding.container.isVisible

    override fun onSendTokenAssetClicked(accountIndex: Int, tokenIndex: Int) = listener.onSendAssetTokenClicked(accountIndex, tokenIndex)
    override fun onSendTokenClicked(account: Account) = listener.onSendAccountClicked(account)

    fun setListener(listener: AccountsAdapterListener) {
        this.listener = listener
    }

    fun setData(index: Int, account: Account) {
        this.rawPosition = index
        view.apply {
            prepareView(account)
            prepareAssets(account)
            bindData(account)
            setOnMenuClickListener(rawPosition, account)
        }

        binding.qrCode.setOnClickListener {
            listener.onShowAddress(account, rawPosition)
        }
    }

    private fun View.bindData(account: Account) {
        with(account) {
            binding.apply {
                card.setCardBackgroundColor(Color.parseColor(NetworkManager.getStringColor(network, isPending)))
                progress.apply {
                    visibleOrGone(isPending)
                    DrawableCompat.setTint(indeterminateDrawable, Color.parseColor(network.color))
                }
                pendingMask.visibleOrGone(isPending)
                mainIcon.setImageDrawable(getNetworkIcon(context, network.short, isSafeAccount))
                accountName.text = name
                //TODO add correct icon here: logoRes = getNetworkIcon(context, network.short, isSafeAccount)
                mainTokenView.initView(account, this@AccountViewHolder)
            }
        }
    }

    private fun prepareView(account: Account) {
        if (!account.isSafeAccount) {
            prepareView()
        } else {
            prepareSafeAccountView()
        }
    }

    private fun prepareView() {
        binding.mainContent.run {
            margin(NO_FRAME, FRAME_TOP_WIDTH, NO_FRAME, NO_FRAME)
            setBackgroundResource(R.drawable.identity_background)
        }
    }

    private fun prepareSafeAccountView() {
        binding.mainContent.run {
            margin(FRAME_WIDTH, FRAME_TOP_WIDTH, FRAME_WIDTH, FRAME_WIDTH)
            setBackgroundResource(R.drawable.safe_account_background)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun View.setOnMenuClickListener(index: Int, account: Account) {
        binding.menu.setOnClickListener {
            PopupMenu(context, binding.menu).apply {
                inflate(R.menu.account_menu)
                menu.findItem(R.id.addSafeAccount).isVisible = isCreatingSafeAccountAvailable(account)
                menu.findItem(R.id.safeAccountSettings).isVisible = isSafeAccount(account)
                setOnItemMenuClickListener(index, account)
            }.also {
                with(MenuPopupHelper(context, it.menu as MenuBuilder, binding.menu)) {
                    setForceShowIcon(true)
                    gravity = Gravity.END
                    show()
                }
            }
        }
    }

    private fun View.setOnItemClickListener(isAssetAreaAvailable: Boolean) =
        setOnClickListener { if (isAssetAreaAvailable) if (isOpen) close() else open() }

    private fun View.prepareAssets(account: Account) {
        binding.apply {
            account.accountAssets.isNotEmpty().let { visible ->
                with(container) {
                    removeAllViews()
                    addView(TokensAndCollectiblesView(viewGroup, account, this@AccountViewHolder, true).apply {
                        visibleOrGone(visible)
                    })
                }
                setOnItemClickListener(visible)
                dividerTop.visibleOrInvisible(visible)
                dividerBottom.visibleOrInvisible(visible)
                arrow.visibleOrGone(visible)
                containerBackground.visibleOrGone(visible)
            }
        }
    }

    private fun open() {
        TransitionManager.beginDelayedTransition(viewGroup)
        binding.apply {
            arrow.rotate180()
            container.visible()
        }
    }

    private fun close() {
        TransitionManager.endTransitions(viewGroup)
        TransitionManager.beginDelayedTransition(viewGroup)
        binding.apply {
            arrow.rotate180back()
            container.gone()
        }
    }

    private fun PopupMenu.setOnItemMenuClickListener(index: Int, account: Account) {
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.walletConnect -> listener.onWalletConnect()
                R.id.safeAccountSettings -> listener.onShowSafeAccountSettings(account, index)
                R.id.addSafeAccount -> listener.onCreateSafeAccountClicked(account)
                R.id.remove -> listener.onAccountRemoved(index)
            }
            true
        }
    }

    private fun isCreatingSafeAccountAvailable(account: Account) =
        !isSafeAccount(account) && account.network.isSafeAccountAvailable

    private fun isSafeAccount(account: Account) = account.network.isAvailable() && account.isSafeAccount

    companion object {
        private const val FRAME_TOP_WIDTH = 3f
        private const val NO_FRAME = 0f
        private const val FRAME_WIDTH = 1.5f
    }
}