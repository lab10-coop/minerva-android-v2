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
import minerva.android.databinding.FragmentManageAssetsBinding
import minerva.android.kotlinUtils.NO_PADDING
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.Asset
import minerva.android.wrapped.WrappedActivity
import minerva.android.wrapped.WrappedActivity.Companion.INDEX
import org.koin.androidx.viewmodel.ext.android.viewModel

class ManageAssetsFragment : BaseFragment(R.layout.fragment_manage_assets) {

    private val viewModel: ManageAssetsViewModel by viewModel()
    private lateinit var showFragmentListener: ShowFragmentListener

    private lateinit var binding: FragmentManageAssetsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentManageAssetsBinding.bind(view)
        showFragmentListener = (activity as WrappedActivity)
        initFragment()
        prepareListeners()
        prepareAssetList()
    }

    private fun prepareListeners() {
        binding.apply {
            rearrangeAssets.setOnClickListener {
                //TODO implement this element
                Toast.makeText(
                    it.context,
                    "This feature will be enabled once we have automated the token discovery.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            addAsset.setOnClickListener {
                showFragmentListener.showFragment(
                    AddAssetFragment.newInstance(),
                    R.animator.slide_in_left,
                    R.animator.slide_out_right,
                    getString(R.string.add_asset)
                )
            }
        }
    }

    private fun prepareAssetList() {
        viewModel.loadAssets().let { assets ->
            binding.assetContainer.apply {
                removeAllViews()
                addMainToken(assets)
                addAssets(assets)
            }
        }
    }

    private fun addMainToken(assets: List<Asset>) {
        binding.assetContainer.apply {
            addView(TextView(requireContext()).apply {
                initTokenRow(this, assets[MAIN_TOKEN_INDEX].name)
            })
        }
    }

    private fun addAssets(assets: List<Asset>) {
        binding.assetContainer.apply {
            assets.drop(FIRST_ELEMENT).forEach { asset ->
                addView(SwitchMaterial(requireContext()).apply {
                    initTokenRow(this, asset.name)
                    isChecked = viewModel.getAssetVisibilitySettings(asset.address)
                    setOnCheckedChangeListener { _, _ -> viewModel.saveAssetVisibilitySettings(asset.address, isChecked) }
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
            ManageAssetsFragment().apply {
                arguments = Bundle().apply {
                    putInt(INDEX, index)
                }
            }
    }
}