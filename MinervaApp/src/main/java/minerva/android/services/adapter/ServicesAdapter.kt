package minerva.android.services.adapter

import android.annotation.SuppressLint
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.service_list_row.view.*
import minerva.android.R
import minerva.android.services.listener.ServicesMenuListener
import minerva.android.walletmanager.model.Service
import minerva.android.widget.repository.getServiceIcon

class ServicesAdapter(private val listener: ServicesMenuListener) : RecyclerView.Adapter<ServiceViewHolder>() {

    private var services = listOf<Service>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder =
        ServiceViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.service_list_row, parent, false), parent, listener)

    override fun getItemCount(): Int = services.size

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bindData(services[position])
    }

    fun updateList(services: List<Service>) {
        this.services = services
        notifyDataSetChanged()
    }
}

class ServiceViewHolder(private val view: View, private val viewGroup: ViewGroup, private val listener: ServicesMenuListener) :
    RecyclerView.ViewHolder(view) {
    @SuppressLint("SetTextI18n")
    fun bindData(service: Service) {
        view.apply {
            showIcon(getServiceIcon(service.type))
            serviceName.text = service.name
            lastUsed.text = "${context.getString(R.string.last_used)} ${service.lastUsed}"

            popupMenu.setOnClickListener { view ->
                PopupMenu(this@ServiceViewHolder.view.context, view).apply {
                    menuInflater.inflate(R.menu.remove_menu, menu)
                    gravity = Gravity.END
                    show()
                    setOnMenuItemClickListener {
                        if (isRemoveItem(it)) listener.onRemoved(service.type, service.name)
                        true
                    }
                }
            }
        }
    }

    private fun isRemoveItem(it: MenuItem) = it.itemId == R.id.remove

    private fun View.showIcon(icon: Int) {
        Glide.with(viewGroup.context).load(icon).into(serviceLogo)
    }
}