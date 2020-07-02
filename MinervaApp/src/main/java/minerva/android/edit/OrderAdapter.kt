package minerva.android.edit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.order_list_row.view.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.extension.visibleOrInvisible
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Network
import minerva.android.widget.repository.getNetworkIcon
import minerva.android.widget.repository.getServiceIcon

class OrderAdapter : RecyclerView.Adapter<OrderViewHolder>() {

    private lateinit var activeList: MutableList<Account>
    private lateinit var inactiveList: List<Account>
    private lateinit var safeAccountsList: List<Account>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder =
        OrderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.order_list_row, parent, false))

    override fun getItemCount(): Int = activeList.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.setData(activeList[position], safeAccountsList)
    }

    fun updateList(data: List<Account>) {
        with(data) {
            activeList = filter { !it.isDeleted && !it.isSafeAccount }.toMutableList()
            inactiveList = filter { it.isDeleted }
            safeAccountsList = filter { !it.isDeleted && it.isSafeAccount }
        }
        notifyDataSetChanged()
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                activeList[i] = activeList.set(i + 1, activeList[i]);
            }
        } else {
            for (i in fromPosition..toPosition + 1) {
                activeList[i] = activeList.set(i - 1, activeList[i])
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getList(): List<Account> = activeList.toMutableList().apply {
        safeAccountsList.asReversed().forEach {
            add(getSafeAccountPosition(it), it)
        }
    } + inactiveList

    private fun getSafeAccountPosition(safeAccount: Account): Int {
        activeList.forEachIndexed { index, element ->
            if (element.address == safeAccount.bindedOwner) return index + 1
        }
        return activeList.size
    }
}

class OrderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    fun setData(element: Account, safeAccounts: List<Account>) {
        view.apply {
            name.text = element.name
            prepareIcon(element)
            prepareSafeAccountLabel(element, safeAccounts)
        }
    }

    private fun prepareIcon(element: Account) {
        view.apply {
            (element.network == String.Empty && element.type == String.Empty).let { isIdentity ->
                letterLogo.visibleOrGone(isIdentity)
                icon.visibleOrInvisible(!isIdentity)
            }

            when {
                element.network != String.Empty -> icon.setImageResource(getNetworkIcon(Network.fromString(element.network)))
                element.type != String.Empty -> icon.setImageResource(getServiceIcon(element.type))
                else -> letterLogo.createLogo(element.name)
            }
        }
    }

    private fun prepareSafeAccountLabel(element: Account, safeAccounts: List<Account>) {
        var safeAccountCount = 0
        var safeAccountLabelText = String.Empty
        safeAccounts.forEach {
            if (it.bindedOwner == element.address) {
                safeAccountCount++
                safeAccountLabelText = it.name
            }
        }
        view.apply {
            safeAccountLabel.visibleOrGone(safeAccountCount > 0)
            when (safeAccountCount) {
                ONE_SAFE_ACCOUNT -> safeAccountLabel.text = safeAccountLabelText
                else -> safeAccountLabel.text = String.format(context.getString(R.string.safe_accounts_count_format), safeAccountCount)
            }
        }
    }

    companion object {
        private const val ONE_SAFE_ACCOUNT = 1
    }
}