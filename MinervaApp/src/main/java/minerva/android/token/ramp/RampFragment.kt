package minerva.android.token.ramp

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import minerva.android.R
import minerva.android.databinding.FragmentRampBinding
import minerva.android.main.base.BaseFragment
import minerva.android.token.ramp.adapter.RampCryptoAdapter
import minerva.android.token.ramp.model.RampCrypto
import org.koin.androidx.viewmodel.ext.android.viewModel

class RampFragment : BaseFragment(R.layout.fragment_ramp) {

    private lateinit var binding: FragmentRampBinding
    private val viewModel: RampViewModel by viewModel()
    private val cryptoAdapter by lazy { RampCryptoAdapter(crypto) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRampBinding.bind(view)
        initializeFragment(view)
    }

    private fun initializeFragment(view: View) {
        binding.apply {
            cryptoRecycler.apply {
                layoutManager = GridLayoutManager(view.context, RAMP_CRYPTO_COLUMNS)
                adapter = cryptoAdapter
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RampFragment()
        private const val RAMP_CRYPTO_COLUMNS = 2
    }

    //TODO klop how to store crypto ramp?
    private val crypto = listOf(
            RampCrypto(1, "ETH", R.drawable.ic_ethereum_token),
            RampCrypto(2, "DAI", R.drawable.ic_dai_token),
            RampCrypto(3, "xDAI", R.drawable.ic_xdai_token),
            RampCrypto(4, "USDC", R.drawable.ic_usdc_token)
    )
}