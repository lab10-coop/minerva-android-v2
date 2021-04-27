package minerva.android.settings.version

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.FragmentAppVersionBinding
import minerva.android.main.base.BaseFragment
import minerva.android.settings.model.ReleaseNote

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
                HeaderNotesAdapter(), ReleaseNoteAdapter(
                    listOf(
                        ReleaseNote("1.0.3", "Some Some notes notes"),
                        ReleaseNote("1.0.4", "WTF klop WTF")
                    )
                )
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AppVersionFragment()
    }
}