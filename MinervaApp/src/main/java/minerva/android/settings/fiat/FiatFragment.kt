package minerva.android.settings.fiat

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.FragmentCurrencyBinding
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.Fiat
import org.koin.androidx.viewmodel.ext.android.viewModel

class FiatFragment : BaseFragment(R.layout.fragment_currency) {

    private lateinit var binding: FragmentCurrencyBinding
    private val viewModel: FiatViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCurrencyBinding.bind(view)
        initializeFragment()
    }

    private fun initializeFragment() {
        binding.fiats.apply {
            layoutManager = LinearLayoutManager(context)
            viewModel.apply {
                adapter = FiatAdapter(Fiat.all, getCurrentFiatPosition()) {
                    saveCurrentFiat(Fiat.all[it])
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = FiatFragment()
    }
}