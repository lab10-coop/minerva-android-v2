package minerva.android.settings.version

import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.FragmentAppVersionBinding
import minerva.android.extension.visibleOrGone
import minerva.android.main.base.BaseFragment

class AppVersionFragment : BaseFragment(R.layout.fragment_app_version) {

    private lateinit var binding: FragmentAppVersionBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppVersionBinding.bind(view)
        initializeFragment()
    }

    private fun initializeFragment() {
        binding.releaseNotes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ConcatAdapter(
                HeaderNotesAdapter(), ReleaseNoteAdapter(context.resources.getStringArray(R.array.release_notes).toList())
            )
            with(binding.footer) {
                visibleOrGone(canScrollVertically(SCROLLING_DOWN))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        visibleOrGone(canScrollVertically(SCROLLING_DOWN))
                    }
                })
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AppVersionFragment()
        private const val SCROLLING_DOWN = 1
    }
}