package minerva.android.token

import android.os.Bundle
import android.view.View
import android.widget.Toast
import minerva.android.R
import minerva.android.accounts.listener.ShowFragmentListener
import minerva.android.databinding.FragmentManageTokensBinding
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token
import minerva.android.widget.token.SwitchTokenRow
import minerva.android.widget.token.TokenRow
import minerva.android.wrapped.WrappedActivity
import minerva.android.wrapped.WrappedActivity.Companion.INDEX
import org.koin.androidx.viewmodel.ext.android.viewModel

class ManageTokensFragment : BaseFragment(R.layout.fragment_manage_tokens) {

    private val viewModel: ManageTokensViewModel by viewModel()
    private lateinit var showFragmentListener: ShowFragmentListener

    private lateinit var binding: FragmentManageTokensBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentManageTokensBinding.bind(view)
        showFragmentListener = (activity as WrappedActivity)
        initFragment()
        prepareListeners()
    }

    override fun onResume() {
        super.onResume()
        prepareTokenList()
    }

    private fun prepareListeners() {
        binding.apply {
            rearrangeTokens.setOnClickListener {
                //TODO implement this element
                Toast.makeText(
                    it.context,
                    "This feature will be enabled once we have automated the token discovery.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            addToken.setOnClickListener {
                viewModel.account.let {
                    showFragmentListener.showFragment(
                        AddTokenFragment.newInstance(it.privateKey, it.network.chainId),
                        R.animator.slide_in_left,
                        R.animator.slide_out_right,
                        getString(R.string.add_asset)
                    )
                }
            }
        }
    }

    private fun prepareTokenList() {
        viewModel.loadTokens().let { tokens ->
            binding.tokenContainer.apply {
                removeAllViews()
                addMainToken(tokens)
                addTokens(tokens)
            }
        }
    }

    private fun addMainToken(tokens: List<Token>) {
        binding.tokenContainer.apply {
            addView(TokenRow(requireContext()).apply {
                initView(tokens[MAIN_TOKEN_INDEX])
            })
        }
    }

    private fun addTokens(tokens: List<Token>) {
        binding.tokenContainer.apply {
            tokens.drop(FIRST_ELEMENT).forEach {
                (it as? ERC20Token)?.let { token ->
                    addView(SwitchTokenRow(requireContext()).apply {
                        initView(token, viewModel.getTokenVisibilitySettings(token.address)) { address, isChecked ->
                            viewModel.saveTokenVisibilitySettings(address, isChecked)
                        }
                    })
                }
            }
        }
    }

    private fun initFragment() {
        arguments?.let {
            viewModel.initViewModel(it.getInt(INDEX))
        }
    }

    companion object {
        private const val MAIN_TOKEN_INDEX = 0
        private const val FIRST_ELEMENT = 1

        @JvmStatic
        fun newInstance(index: Int) =
            ManageTokensFragment().apply {
                arguments = Bundle().apply {
                    putInt(INDEX, index)
                }
            }
    }
}