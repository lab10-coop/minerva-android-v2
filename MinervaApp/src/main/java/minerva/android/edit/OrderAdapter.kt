package minerva.android.edit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.order_list_row.view.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.loadImageUrl
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.widget.LetterLogo
import minerva.android.widget.ProfileImage
import minerva.android.widget.repository.getNetworkIcon
import minerva.android.widget.repository.getServiceIcon

class OrderAdapter : RecyclerView.Adapter<OrderViewHolder>() {

    private lateinit var activeList: MutableList<MinervaPrimitive>
    private lateinit var inactiveList: List<MinervaPrimitive>
    private lateinit var safeAccountsList: List<MinervaPrimitive>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder =
        OrderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.order_list_row, parent, false))

    override fun getItemCount(): Int = activeList.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.setData(activeList[position], safeAccountsList)
    }

    fun updateList(data: List<MinervaPrimitive>) {
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

    fun getList(): List<MinervaPrimitive> = activeList.toMutableList().apply {
        safeAccountsList.asReversed().forEach {
            add(getSafeAccountPosition(it), it)
        }
    } + inactiveList

    private fun getSafeAccountPosition(safeAccount: MinervaPrimitive): Int {
        activeList.forEachIndexed { index, element ->
            if (element.address == safeAccount.bindedOwner) return index + 1
        }
        return activeList.size
    }
}

class OrderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    fun setData(element: MinervaPrimitive, safeAccounts: List<MinervaPrimitive>) {
        view.apply {
            name.text = element.name
            prepareIcon(element)
            prepareSafeAccountLabel(element, safeAccounts)
        }
    }

    private fun prepareIcon(element: MinervaPrimitive) {
        view.apply {
            when {
                element.network != String.Empty -> icon.setImageResource(getNetworkIcon(NetworkManager.getNetwork(element.network)))
                element.type != String.Empty && element !is Credential -> icon.setImageResource(getServiceIcon(element.type))
                element is Credential -> icon.loadImageUrl(element.iconUrl)
                else -> ProfileImage.load(icon, element as Identity)
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