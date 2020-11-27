package minerva.android.accounts.adapter

import android.annotation.SuppressLint
import android.content.Context
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
import kotlinx.android.synthetic.main.account_list_row.view.*
import minerva.android.R
import minerva.android.accounts.listener.AccountsAdapterListener
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.utils.BalanceUtils
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.widget.AssetView
import minerva.android.widget.repository.getNetworkIcon
import java.math.BigDecimal

class AccountViewHolder(private val view: View, private val parent: ViewGroup) : AssetView.AssertViewCallback,
    RecyclerView.ViewHolder(view) {

    private lateinit var listener: AccountsAdapterListener
    private var rawPosition: Int = Int.InvalidIndex

    private val isOpen
        get() = view.sendButton.isVisible

    override fun onSendAssetClicked(accountIndex: Int, assetIndex: Int) = listener.onSendAssetClicked(accountIndex, assetIndex)

    override val viewGroup: ViewGroup
        get() = parent

    override val context: Context
        get() = view.context

    fun setListener(listener: AccountsAdapterListener) {
        this.listener = listener
    }

    fun setData(rawPosition: Int, account: Account) {
        this.rawPosition = rawPosition
        view.apply {
            bindData(account)
            prepareView(account)
            prepareAssets(account)
            setOnSendButtonClickListener(account)
            setOnMenuClickListener(rawPosition, account)
            setOnItemClickListener()
            qrCode.setOnClickListener {
                listener.onShowAddress(account, rawPosition)
            }
        }
    }

    private fun View.bindData(account: Account) {
        with(account) {
            card.setCardBackgroundColor(Color.parseColor(NetworkManager.getStringColor(network, isPending)))
            progress.apply {
                visibleOrGone(isPending)
                DrawableCompat.setTint(indeterminateDrawable, Color.parseColor(network.color))
            }
            pendingMask.visibleOrGone(isPending)
            icon.setImageDrawable(getNetworkIcon(context, network.short, isSafeAccount))
            accountName.text = name
            cryptoTokenName.run {
                text = network.token
                setTextColor(Color.parseColor(network.color))
            }
            with(amountView) {
                setCrypto(BalanceUtils.getCryptoBalance(cryptoBalance))
                (if (network.testNet) BigDecimal.ZERO
                else account.fiatBalance).let { setFiat(BalanceUtils.getFiatBalance(it)) }
            }
            sendButton.text = String.format(SEND_BUTTON_FORMAT, context.getString(R.string.send), network.token)
        }
    }

    private fun View.prepareView(account: Account) {
        if (!account.isSafeAccount) {
            prepareView()
        } else {
            prepareSafeAccountView()
        }
    }

    private fun View.prepareView() {
        mainContent.run {
            margin(NO_FRAME, FRAME_TOP_WIDTH, NO_FRAME, NO_FRAME)
            setBackgroundResource(R.drawable.identity_background)
        }
    }

    private fun View.prepareSafeAccountView() {
        mainContent.run {
            margin(FRAME_WIDTH, FRAME_TOP_WIDTH, FRAME_WIDTH, FRAME_WIDTH)
            setBackgroundResource(R.drawable.safe_account_background)
        }
    }

    private fun View.setOnSendButtonClickListener(account: Account) {
        sendButton.setOnClickListener {
            listener.onSendAccountClicked(account)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun View.setOnMenuClickListener(rawPosition: Int, account: Account) {
        menu.setOnClickListener {
            PopupMenu(context, menu).apply {
                inflate(R.menu.account_menu)
                menu.findItem(R.id.addSafeAccount).isVisible = isCreatingSafeAccountAvailable(account)
                menu.findItem(R.id.safeAccountSettings).isVisible = isSafeAccount(account)
                setOnItemMenuClickListener(rawPosition, account)
            }.also {
                with(MenuPopupHelper(context, it.menu as MenuBuilder, menu)) {
                    setForceShowIcon(true)
                    gravity = Gravity.END
                    show()
                }
            }
        }
    }

    private fun View.setOnItemClickListener() {
        setOnClickListener {
            if (isOpen) close() else open()
        }
    }

    private fun View.prepareAssets(account: Account) {
        container.removeAllViews()
        account.accountAssets.forEachIndexed { index, asset ->
            container.addView(AssetView(this@AccountViewHolder, account, index, R.drawable.ic_asset_sdai).apply {
                setAmounts(asset.balance)
            })
        }
    }

    private fun open() {
        TransitionManager.beginDelayedTransition(viewGroup)
        view.apply {
            arrow.rotate180()
            sendButton.visible()
            container.visible()
        }
    }

    private fun close() {
        TransitionManager.endTransitions(viewGroup)
        TransitionManager.beginDelayedTransition(viewGroup)
        view.apply {
            arrow.rotate180back()
            sendButton.gone()
            container.gone()
        }
    }

    private fun PopupMenu.setOnItemMenuClickListener(position: Int, account: Account) {
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.walletConnect -> listener.onWalletConnect()
                R.id.safeAccountSettings -> listener.onShowSafeAccountSettings(account, position)
                R.id.addSafeAccount -> listener.onCreateSafeAccountClicked(account)
                R.id.remove -> listener.onAccountRemoved(position)
            }
            true
        }
    }

    private fun isCreatingSafeAccountAvailable(account: Account) =
        !isSafeAccount(account) && account.network.isSafeAccountAvailable

    private fun isSafeAccount(account: Account) = account.network.isAvailable() && account.isSafeAccount

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
        private const val FRAME_TOP_WIDTH = 3f
        private const val NO_FRAME = 0f
        private const val FRAME_WIDTH = 1.5f
    }
}