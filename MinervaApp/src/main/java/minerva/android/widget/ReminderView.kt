package minerva.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.reminder_view.view.*
import minerva.android.R
import minerva.android.settings.adapter.AlertsPagerAdapter
import minerva.android.settings.model.SettingRow

@SuppressLint("ViewConstructor")
class ReminderView(context: Context, attrs: AttributeSet? = null, rows: List<SettingRow>) : LinearLayout(context, attrs) {

    private val adapter: AlertsPagerAdapter by lazy { AlertsPagerAdapter() }

    init {
        inflate(context, R.layout.reminder_view, this)
        setBackgroundResource(R.drawable.rounded_white_background)
        val dimen = resources.getDimension(R.dimen.margin_small).toInt()
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(dimen, dimen, dimen, dimen)
        layoutParams = params
        setPadding(dimen, dimen, dimen, dimen)
        elevation = 4f
        orientation = VERTICAL
        reminderViewpager.adapter = adapter
        adapter.updateAlerts(rows)
        if (rows.size > 1) {
            TabLayoutMediator(tabLayout, reminderViewpager) { _, _ -> }.attach()
        }
    }
}