package minerva.android.settings.advanced

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.FragmentAdvancedBinding
import minerva.android.kotlinUtils.NO_MARGIN
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.settings.advanced.adapter.AdvancedAdapter
import minerva.android.settings.advanced.model.AdvancedSection
import minerva.android.settings.advanced.model.AdvancedSectionRowType
import minerva.android.settings.dialog.TokenResetDialog
import minerva.android.utils.VerticalMarginItemDecoration
import org.koin.androidx.viewmodel.ext.android.viewModel

class AdvancedFragment : BaseFragment(R.layout.fragment_advanced) {

    private lateinit var binding: FragmentAdvancedBinding
    val viewModel: AdvancedViewModel by viewModel()

    private val dialog: TokenResetDialog by lazy {
        TokenResetDialog(requireContext()) {
            viewModel.resetTokens()
        }
    }

    private val advancedAdapter by lazy {
        AdvancedAdapter { onSectionClicked(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAdvancedBinding.bind(view)
        setupRecycleView()
        setupObservers()
    }

    /**
    * Get Advanced Sections - parse advanced sections data by viewModel for getting/filling necessary data
    * @return list with parsed data
    */
    private fun getAdvancedSections(): List<AdvancedSection> =
        listOf<AdvancedSection>(
            AdvancedSection(
                title = R.string.clear_token_cache_title,
                description = R.string.clear_token_cache_description,
                actionText = R.string.clear_token_cache_action_text,
                rowType = AdvancedSectionRowType.CLEAR_TOKEN_CACHE,
            ),
            AdvancedSection(
                title = R.string.change_network_prompt_title,
                description = R.string.change_network_prompt_description,
                rowType = AdvancedSectionRowType.CHANGE_NETWORK_PROMPT,
                isSwitchChecked = viewModel.isChangeNetworkEnabled
            )
        )

    private fun setupObservers() = with(viewModel){
        resetTokensLiveData.observe(viewLifecycleOwner, EventObserver { handleResetTokenState(it) })
    }

    private fun setupRecycleView() {
        binding.sections.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = advancedAdapter
            advancedAdapter.updateList(getAdvancedSections())
            addItemDecoration(getRecyclerViewItemDecorator())
        }
    }

    private fun getRecyclerViewItemDecorator(): VerticalMarginItemDecoration {
        val margin = requireContext().resources.getDimension(R.dimen.margin_small).toInt()
        return VerticalMarginItemDecoration(margin, Int.NO_MARGIN, Int.NO_MARGIN)
    }

    companion object {
        @JvmStatic
        fun newInstance() = AdvancedFragment()
    }

    private fun onSectionClicked(sectionRowType: AdvancedSectionRowType) {
        when(sectionRowType){
            AdvancedSectionRowType.CLEAR_TOKEN_CACHE -> onClearTokenCacheClicked()
            AdvancedSectionRowType.CHANGE_NETWORK_PROMPT -> viewModel.changeStateOfChangeNetworkEnabled()
        }
    }

    private fun openTokenResetDialog() {
        dialog.show()
    }

    private fun handleResetTokenState(state: Result<Any>) {
        when {
            state.isSuccess -> dialog.dismiss()
            state.isFailure -> dialog.showError(state.exceptionOrNull())
        }
    }

    private fun onClearTokenCacheClicked(){
        openTokenResetDialog()
    }
}