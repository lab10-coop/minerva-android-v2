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
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.repository.getNetworkIcon
import minerva.android.widget.state.AccountWidgetState
import minerva.android.widget.token.TokenView

class AccountViewHolder(private val view: View, private val viewGroup: ViewGroup) : TokenView.TokenViewCallback,
    RecyclerView.ViewHolder(view) {

    private var binding = AccountListRowBinding.bind(view)

    private lateinit var listener: AccountsAdapterListener
    private var rawPosition: Int = Int.InvalidIndex

    private val isWidgetOpen
        get() = binding.tokensAndCollectibles.isVisible

    private val accountWidgetState: AccountWidgetState
        get() = listener.getAccountWidgetState(rawPosition)

    override fun onSendTokenTokenClicked(account: Account, tokenIndex: Int) = listener.onSendTokenClicked(account, tokenIndex)

    override fun onSendTokenClicked(account: Account) = listener.onSendAccountClicked(account)

    fun setData(index: Int, account: Account, fiatSymbol: String, listener: AccountsAdapterListener) {
        rawPosition = index
        this.listener = listener
        view.apply {
            prepareView(account)
            prepareToken(account, fiatSymbol)
            bindData(account, fiatSymbol)
            setOnMenuClickListener(rawPosition, account)
        }

        binding.qrCode.setOnClickListener {
            listener.onShowAddress(account)
        }
    }

    private fun View.bindData(account: Account, fiatSymbol: String) {
        with(account) {
            binding.apply {
                card.setCardBackgroundColor(Color.parseColor(NetworkManager.getStringColor(network, isPending)))
                progress.apply {
                    visibleOrGone(isPending)
                    DrawableCompat.setTint(indeterminateDrawable, Color.parseColor(network.color))
                }
                pendingMask.visibleOrGone(isPending)
                mainIcon.setImageDrawable(getNetworkIcon(context, network.chainId, isSafeAccount))
                accountName.text = name
                mainTokenView.initView(account, this@AccountViewHolder, fiatSymbol)
            }
        }
    }

    private fun prepareView(account: Account) {
        if (!account.isSafeAccount) prepareAccountView()
        else prepareSafeAccountView()
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
        setOnClickListener { if (isTokenAreaAvailable) if (isWidgetOpen) close() else open() }

    private fun View.prepareToken(account: Account, fiatSymbol: String) {
        binding.apply {
            tokensAndCollectibles.prepareView(
                account,
                viewGroup,
                this@AccountViewHolder,
                accountWidgetState.isWidgetOpen,
                fiatSymbol
            )
            //TODO change this statement when collectibles or main coin will be implemented
            account.accountTokens.isNotEmpty().let { visible ->
                setOnItemClickListener(visible)
                dividerTop.visibleOrInvisible(visible)
                dividerBottom.visibleOrInvisible(visible)
                arrow.visibleOrGone(visible)
                containerBackground.visibleOrGone(visible)
            }
        }
    }

    private fun setOpen(isOpen: Boolean) {
        accountWidgetState.isWidgetOpen = isOpen
        listener.updateAccountWidgetState(rawPosition, accountWidgetState)
        binding.apply {
            if (isOpen) arrow.rotate180() else arrow.rotate180back()
            tokensAndCollectibles.visibleOrGone(isOpen)
        }
    }

    private fun open() {
        TransitionManager.beginDelayedTransition(viewGroup)
        setOpen(true)
    }

    private fun close() {
        TransitionManager.endTransitions(viewGroup)
        TransitionManager.beginDelayedTransition(viewGroup)
        setOpen(false)
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

    private fun isCreatingSafeAccountAvailable(account: Account) =
        !isSafeAccount(account) && account.network.isSafeAccountAvailable

    private fun isExportSafeAccountAvailable(account: Account) = !isSafeAccount(account)

    private fun isSafeAccount(account: Account) =
        account.network.isAvailable() && account.isSafeAccount

    companion object {
        private const val FRAME_TOP_WIDTH = 3f
        private const val NO_FRAME = 0f
        private const val FRAME_WIDTH = 1.5f
    }
}