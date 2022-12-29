package minerva.android.token

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import minerva.android.R
import minerva.android.accounts.listener.ShowFragmentListener
import minerva.android.databinding.FragmentManageTokensBinding
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.token.ERCToken
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
        initViewModel()
        prepareListeners()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onResume() {
        super.onResume()
        prepareTokenList()
    }

    private fun prepareListeners() {
        binding.apply {
            addToken.setOnClickListener {
                viewModel.account.let { account ->
                    showFragmentListener.showFragment(
                        AddTokenFragment.newInstance(account.privateKey, account.network.chainId, account.address),
                        R.animator.slide_in_left,
                        R.animator.slide_out_right,
                        getString(R.string.add_asset)
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun prepareTokenList() {
        viewModel.loadTokens().let { tokens ->
            binding.tokenContainer.apply {
                removeAllViews()
                addMainToken(tokens)
                addTokens(tokens as List<ERCToken>)
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

    @RequiresApi(Build.VERSION_CODES.N)
    private fun addTokens(tokens: List<ERCToken>) {
        //filtered tokens list for deleting main network token (it should be display separately)
        val filteredTokens: List<ERCToken> = tokens.drop(FIRST_ELEMENT)
        //get type of first element for hide/show separator(between tokens) when type (isERC20() || isNFT()) changes; saves previous type buffer state
        var typeBuffer: Boolean = false
        if (filteredTokens.isNotEmpty())
            typeBuffer = filteredTokens.get(Int.FirstIndex).type.isERC20()
        //buffer for showing (some) previous state of separator view; saves previous logo buffer state
        var logoBuffer: Boolean = true

        binding.tokenContainer.apply {
            filteredTokens.forEachIndexed { index, currentToken ->
                var separator: Boolean = false//using for hiding/showing separator(between tokens) ("@+id/type_separator")
                if (typeBuffer == currentToken.type.isERC20()) {//if token type changed (between isERC20() - isNFT()) we would show separator
                    if (currentToken.logoURI.isNullOrEmpty() && logoBuffer) {//logic for showing separator when tokens with logos(url doesn't empty) ended
                        if (index > Int.FirstIndex) {//for prevent calling empty item of list
                            val previousToken: ERCToken = filteredTokens.get(index - FIRST_ELEMENT)//get previous token for comparing logo state
                            if (!previousToken.logoURI.isNullOrEmpty()) {//check that current group already showed separator(prevent multi separator showing)
                                separator = true
                                logoBuffer = false//preventing multiple separator showing for one group
                            }
                        }
                    }
                } else {
                    typeBuffer = currentToken.type.isERC20()//if type changed buffer would be change too
                    logoBuffer = true
                    separator = true
                }
                currentToken.let { token ->
                    addView(SwitchTokenRow(requireContext()).apply {
                        initView(token, viewModel.getTokenVisibilitySettings(token.address), separator) { address, isChecked ->
                            viewModel.saveTokenVisibilitySettings(address, isChecked)
                        }
                    })
                }
            }
        }
    }

    private fun initViewModel() {
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