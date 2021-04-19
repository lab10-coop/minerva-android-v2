package minerva.android.settings.currency

import android.os.Bundle
import android.view.View
import minerva.android.R
import minerva.android.databinding.FragmentCurrencyBinding
import minerva.android.main.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class CurrencyFragment : BaseFragment(R.layout.fragment_currency) {

    private lateinit var binding: FragmentCurrencyBinding
    private val viewModel: CurrencyViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCurrencyBinding.bind(view)
        initializeFragment()
    }

    private fun initializeFragment() {
        viewModel.getCurrentCurrency()
    }

    companion object {
        @JvmStatic
        fun newInstance() = CurrencyFragment()
    }
}