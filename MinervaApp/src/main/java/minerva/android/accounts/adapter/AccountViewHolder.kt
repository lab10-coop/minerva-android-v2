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
import minerva.android.widget.token.TokenView
import minerva.android.widget.TokensAndCollectiblesView
import minerva.android.widget.repository.getNetworkIcon

class AccountViewHolder(private val view: View, private val viewGroup: ViewGroup) :
    TokenView.TokenViewCallback,
    RecyclerView.ViewHolder(view) {

    private var binding = AccountListRowBinding.bind(view)

    private lateinit var listener: AccountsAdapterListener
    private var rawPosition: Int = Int.InvalidIndex

    private val isOpen
        get() = binding.container.isVisible

    override fun onSendTokenTokenClicked(account: Account, tokenIndex: Int) =
        listener.onSendTokenClicked(account, tokenIndex)

    override fun onSendTokenClicked(account: Account) = listener.onSendAccountClicked(account)

    fun setListener(listener: AccountsAdapterListener) {
        this.listener = listener
    }

    fun setData(index: Int, account: Account, isOpen: Boolean) {
        rawPosition = index
        view.apply {
            prepareView(account)
            prepareToken(account)
            bindData(account)
            setOnMenuClickListener(rawPosition, account)
            if (isOpen) binding.container.visible()
        }

        binding.qrCode.setOnClickListener {
            listener.onShowAddress(account)
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
                mainTokenView.initView(account, this@AccountViewHolder)
            }
        }
    }

    private fun prepareView(account: Account) {
        if (!account.isSafeAccount) {
            prepareAccountView()
        } else {
            prepareSafeAccountView()
        }
    }

    private fun prepareAccountView() {
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
                setMenuItems(account)
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

    private fun PopupMenu.setMenuItems(account: Account) {
        with(menu) {
            findItem(R.id.addSafeAccount).isVisible = isCreatingSafeAccountAvailable(account)
            findItem(R.id.exportPrivateKey).isVisible = isExportSafeAccountAvailable(account)
            findItem(R.id.safeAccountSettings).isVisible = isSafeAccount(account)
            if (account.dappSessionCount != 0) {
                findItem(R.id.walletConnect).title =
                    view.context.getString(
                        R.string.wallet_connect_with_count_title,
                        account.dappSessionCount
                    )
            }
        }
    }

    private fun View.setOnItemClickListener(isTokenAreaAvailable: Boolean) =
        setOnClickListener { if (isTokenAreaAvailable) if (isOpen) close() else open() }

    private fun View.prepareToken(account: Account) {
        binding.apply {
            account.accountTokens.isNotEmpty().let { visible ->
                with(container) {
                    removeAllViews()
                    //TODO showing/hiding main token in TokensAndCollectiblesView is made using last argument - needs to be updated in the future
                    addView(
                        TokensAndCollectiblesView(
                            viewGroup,
                            account,
                            this@AccountViewHolder,
                            false
                        ).apply {
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
        listener.onOpenOrClose(rawPosition, true)
        TransitionManager.beginDelayedTransition(viewGroup)
        binding.apply {
            arrow.rotate180()
            container.visible()
        }
    }

    private fun close() {
        listener.onOpenOrClose(rawPosition, false)
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
                R.id.walletConnect -> listener.onWalletConnect(index)
                R.id.manageTokens -> listener.onManageTokens(index)
                R.id.safeAccountSettings -> listener.onShowSafeAccountSettings(account, index)
                R.id.addSafeAccount -> listener.onCreateSafeAccountClicked(account)
                R.id.exportPrivateKey -> listener.onExportPrivateKey(account)
                R.id.remove -> listener.onAccountRemoved(index)
            }
            true
        }
    }

    private fun isCreatingSafeAccountAvailable(account: Account) = !isSafeAccount(account) && account.network.isSafeAccountAvailable

    private fun isExportSafeAccountAvailable(account: Account) = !isSafeAccount(account)

    private fun isSafeAccount(account: Account) =
        account.network.isAvailable() && account.isSafeAccount

    companion object {
        private const val FRAME_TOP_WIDTH = 3f
        private const val NO_FRAME = 0f
        private const val FRAME_WIDTH = 1.5f
    }
}