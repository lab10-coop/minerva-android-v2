package minerva.android.manage

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import minerva.android.R
import minerva.android.accounts.listener.ShowFragmentListener
import minerva.android.databinding.FragmentManageTokensBinding
import minerva.android.kotlinUtils.NO_PADDING
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.Token
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
                        AddTokenFragment.newInstance(it.privateKey, it.network.short),
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
            addView(TextView(requireContext()).apply {
                initTokenRow(this, tokens[MAIN_TOKEN_INDEX].name)
            })
        }
    }

    private fun addTokens(tokens: List<Token>) {
        binding.tokenContainer.apply {
            tokens.drop(FIRST_ELEMENT).forEach { token ->
                addView(SwitchMaterial(requireContext()).apply {
                    initTokenRow(this, token.name)
                    isChecked = viewModel.getTokenVisibilitySettings(token.address)
                    setOnCheckedChangeListener { _, _ -> viewModel.saveTokenVisibilitySettings(token.address, isChecked) }
                })
            }
        }
    }

    private fun initTokenRow(view: TextView, title: String) {
        view.apply {
            text = title
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            resources.getDimension(R.dimen.margin_xsmall).toInt().let { padding ->
                if (this !is SwitchMaterial) setPadding(Int.NO_PADDING, padding, Int.NO_PADDING, padding)
                compoundDrawablePadding = padding
            }
            setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_default_token),
                null,
                null,
                null
            )
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