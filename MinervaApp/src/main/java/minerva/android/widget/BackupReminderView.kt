package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import minerva.android.R

class BackupReminderView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    init {
        inflate(context, R.layout.backup_reminder_view, this)
        setBackgroundResource(R.drawable.rounded_white_background)
        val dimen = resources.getDimension(R.dimen.margin_small).toInt()
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(dimen, dimen, dimen, dimen)
        layoutParams = params
        setPadding(dimen, dimen, dimen, dimen)
        elevation = 2f
        orientation = VERTICAL
    }
}