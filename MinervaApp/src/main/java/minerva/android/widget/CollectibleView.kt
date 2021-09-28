package minerva.android.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import minerva.android.R
import minerva.android.databinding.CollectibleViewBinding
import minerva.android.walletmanager.model.token.ERCToken
import java.math.BigDecimal

@SuppressLint("ViewConstructor")
class CollectibleView(context: Context, collectible: ERCToken, balance: BigDecimal) : ConstraintLayout(context) {

    private var binding = CollectibleViewBinding.bind(inflate(context, R.layout.collectible_view, this))

    init {
        binding.apply {
            collectibleName.text = collectible.symbol
            collectibleDesc.text = collectible.collectionName
            collectibleItem.text = balance.toEngineeringString()
        }
    }
}