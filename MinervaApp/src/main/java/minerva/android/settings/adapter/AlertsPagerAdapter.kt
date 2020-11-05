package minerva.android.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.alert_view.view.*
import minerva.android.R
import minerva.android.settings.model.SettingRow

class AlertsPagerAdapter : RecyclerView.Adapter<AlertViewHolder>() {

    private var alerts: List<SettingRow> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder =
        AlertViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.alert_view, parent, false))

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.seData(alerts[position])
    }

    fun updateAlerts(rows: List<SettingRow>) {
        this.alerts = rows
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = alerts.size
}

class AlertViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun seData(row: SettingRow) {
        with(view) {
            alertTitle.text = row.name
            alertMessage.text = row.detailText
        }
    }
}