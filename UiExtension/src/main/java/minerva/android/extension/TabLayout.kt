package minerva.android.extension

import com.google.android.material.tabs.TabLayout

fun TabLayout.onTabSelected(onTabSelected: (Int) -> Unit) {
    this.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabReselected(tab: TabLayout.Tab?) {}
        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.let { onTabSelected.invoke(it.position) }
        }
    })
}