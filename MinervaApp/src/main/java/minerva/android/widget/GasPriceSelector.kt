package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import minerva.android.R
import minerva.android.databinding.GasPriceSelectorBinding
import minerva.android.extension.invisible
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.main.walletconnect.transaction.TransactionSpeedAdapter
import minerva.android.walletmanager.model.defs.TxType
import minerva.android.walletmanager.model.transactions.TxSpeed

class GasPriceSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: GasPriceSelectorBinding =
        GasPriceSelectorBinding.bind(inflate(context, R.layout.gas_price_selector, this))
    private lateinit var adapter: TransactionSpeedAdapter

    fun setAdapter(speeds: List<TxSpeed>, recalculateTxCost: (TxSpeed) -> Unit) = with(binding) {
        adapter = TransactionSpeedAdapter()
        if (speeds.size == 1) tabLayout.invisible()
        txSpeedViewPager.adapter = adapter
        adapter.updateSpeeds(speeds)
        TabLayoutMediator(tabLayout, txSpeedViewPager) { _, _ -> }.attach()
        (txSpeedViewPager.children.first() as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER


        with(binding) {
            overlay.setOnClickListener {
                if (txSpeedViewPager.currentItem == MAX_POSITION) {
                    txSpeedViewPager.setCurrentItem(MIN_POSITION, false)
                } else {
                    txSpeedViewPager.currentItem = txSpeedViewPager.currentItem + 1
                }
                recalculateTxCost(adapter.getCurrentTxSpeed(txSpeedViewPager.currentItem))
            }
        }
    }

    fun setDefaultPosition(txType: TxType) = with(binding) {
        val position = adapter.getPositionOfTxType(txType)
        if (position != Int.InvalidIndex) {
            txSpeedViewPager.currentItem = position
        }
    }

    companion object {
        private const val MAX_POSITION = 3
        private const val MIN_POSITION = 0
    }
}