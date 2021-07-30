package minerva.android.onboarding.welcome

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.ViewOnBoardingBinding

class OnBoardingAdapter : RecyclerView.Adapter<OnBoardingViewHolder>() {

    private val onBoardingViewData: List<OnBoardingViewData> = listOf(
        OnBoardingViewData(R.string.your_identity_label, R.drawable.on_boarding_identity),
        OnBoardingViewData(R.string.your_data_label, R.drawable.on_boarding_data),
        OnBoardingViewData(R.string.your_money_label, R.drawable.on_boarding_money),
        OnBoardingViewData(R.string.your_own_them_label, R.drawable.on_boarding_own_them)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnBoardingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return OnBoardingViewHolder(ViewOnBoardingBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: OnBoardingViewHolder, position: Int) {
        holder.bindView(onBoardingViewData[position])
    }

    override fun getItemCount(): Int = onBoardingViewData.size
}

class OnBoardingViewHolder(private val binding: ViewOnBoardingBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindView(bindData: OnBoardingViewData) = with(binding) {
        onBoardingImage.setImageResource(bindData.drawableId)
        onBoardingText.setText(bindData.stringId)
    }
}