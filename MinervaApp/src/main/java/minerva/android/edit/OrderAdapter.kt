package minerva.android.edit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.OrderListRowBinding
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.loadImageUrl
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.*
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.widget.ProfileImage
import minerva.android.widget.repository.getNetworkIcon

class OrderAdapter : RecyclerView.Adapter<OrderViewHolder>() {

    private lateinit var displayedList: MutableList<MinervaPrimitive>
    private lateinit var inactiveList: List<MinervaPrimitive>
    private lateinit var safeAccountsList: List<MinervaPrimitive>
    private lateinit var inactiveAccountList: List<MinervaPrimitive>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder =
        OrderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.order_list_row, parent, false))

    override fun getItemCount(): Int = displayedList.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.setData(displayedList[position], safeAccountsList)
    }

    fun updateList(data: List<MinervaPrimitive>, areMainNetsEnabled: Boolean) {
        with(data) {
            displayedList =
                filter { !it.isDeleted && !it.isSafeAccount && areAccountsActive(it, areMainNetsEnabled) }.toMutableList()
            inactiveList = filter { it.isDeleted }
            safeAccountsList = filter { !it.isDeleted && it.isSafeAccount }
            inactiveAccountList = filter { !it.isDeleted && !it.isSafeAccount && filterInactiveAccounts(it, areMainNetsEnabled) }
        }
        notifyDataSetChanged()
    }

    private fun filterInactiveAccounts(it: MinervaPrimitive, areMainNetsEnabled: Boolean) =
        if (it is Account) it.network.testNet == areMainNetsEnabled
        else false

    private fun areAccountsActive(it: MinervaPrimitive, areMainNetsEnabled: Boolean) =
        if (it is Account) it.network.testNet != areMainNetsEnabled
        else true

    fun swapItems(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                displayedList[i] = displayedList.set(i + 1, displayedList[i]);
            }
        } else {
            for (i in fromPosition..toPosition + 1) {
                displayedList[i] = displayedList.set(i - 1, displayedList[i])
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getList(): List<MinervaPrimitive> =
        displayedList.toMutableList().apply {
            safeAccountsList.asReversed().forEach {
                add(getSafeAccountPosition(it), it)
            }
        } + inactiveList + inactiveAccountList

    private fun getSafeAccountPosition(safeAccount: MinervaPrimitive): Int {
        displayedList.forEachIndexed { index, element ->
            if (element.address == safeAccount.bindedOwner) return index + 1
        }
        return displayedList.size
    }
}

class OrderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private var binding = OrderListRowBinding.bind(view)

    fun setData(element: MinervaPrimitive, safeAccounts: List<MinervaPrimitive>) {
        binding.apply {
            name.text = element.name
            prepareIcon(element)
            prepareSafeAccountLabel(element, safeAccounts)
        }
    }

    private val context: Context
        get() = view.context

    private fun prepareIcon(element: MinervaPrimitive) {
        binding.apply {
            when (element) {
                is Account -> mainIcon.setImageDrawable(getNetworkIcon(context, element.network.chainId))
                is Service -> mainIcon.loadImageUrl(element.iconUrl, R.drawable.ic_services)
                is Credential -> mainIcon.loadImageUrl(element.iconUrl, R.drawable.ic_default_credential)
                else -> ProfileImage.load(mainIcon, element as Identity)
            }
        }
    }

    private fun prepareSafeAccountLabel(element: MinervaPrimitive, safeAccounts: List<MinervaPrimitive>) {
        var safeAccountCount = 0
        var safeAccountLabelText = String.Empty
        safeAccounts.forEach {
            if (it.bindedOwner == element.address) {
                safeAccountCount++
                safeAccountLabelText = it.name
            }
        }
        binding.apply {
            safeAccountLabel.visibleOrGone(safeAccountCount > 0)
            when (safeAccountCount) {
                ONE_SAFE_ACCOUNT -> safeAccountLabel.text = safeAccountLabelText
                else -> safeAccountLabel.text =
                    String.format(context.getString(R.string.safe_accounts_count_format), safeAccountCount)
            }
        }
    }

    companion object {
        private const val ONE_SAFE_ACCOUNT = 1
    }
}