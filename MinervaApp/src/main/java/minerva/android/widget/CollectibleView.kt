package minerva.android.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout

import minerva.android.R
import minerva.android.databinding.CollectibleViewBinding
import minerva.android.walletmanager.model.Collectible

//TODO Collectible is the prototype
@SuppressLint("ViewConstructor")
class CollectibleView(context: Context, collectible: Collectible) : ConstraintLayout(context) {

    private var binding = CollectibleViewBinding.bind(inflate(context, R.layout.collectible_view, this))

    init {
        binding.apply {
            collectibleName.text = collectible.name
            collectibleDesc.text = collectible.type
            collectibleItem.text = collectible.items.toString()
        }
    }
}