package minerva.android.settings.advanced

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.FragmentAdvancedBinding
import minerva.android.databinding.FragmentAppVersionBinding
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.NO_MARGIN
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.settings.SettingsViewModel
import minerva.android.settings.adapter.SettingsAdapter
import minerva.android.settings.advanced.adapter.AdvancedAdapter
import minerva.android.settings.advanced.model.AdvancedSectionRowType
import minerva.android.settings.advanced.model.propagateSections
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
        propagateSections()
        setupObservers()
    }

    private fun setupObservers() = with(viewModel){
        resetTokensLiveData.observe(viewLifecycleOwner, EventObserver { handleResetTokenState(it) })
    }

    private fun setupRecycleView() {
        binding.sections.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = advancedAdapter
            advancedAdapter.updateList(propagateSections())
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