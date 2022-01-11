package minerva.android.services.dapps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.services.dapps.model.Dapp
import minerva.android.services.dapps.model.DappsWithCategories
import minerva.android.walletmanager.model.dapps.DappUIDetails
import minerva.android.walletmanager.repository.dapps.DappsRepository

class DappsViewModel(
    private val dappsRepository: DappsRepository
) : BaseViewModel() {
    private val _dappsLiveData: MutableLiveData<DappsWithCategories> = MutableLiveData()
    val dappsLiveData: LiveData<DappsWithCategories> get() = _dappsLiveData

    fun getDapps() {
        launchDisposable {
            dappsRepository.getAllDappsDetails()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.mapToDappList() }
                .subscribeBy { dapps ->
                    _dappsLiveData.value = DappsWithCategories(
                        sponsored = dapps.filter { it.isSponsored }.sortedBy { it.sponsoredOrder },
                        remaining = dapps.filter { !it.isSponsored }.sortedBy { it.shortName }
                    )
                }
        }
    }

    private fun List<DappUIDetails>.mapToDappList(): List<Dapp> =
        map { dappUIDetails ->
            Dapp(
                dappUIDetails.shortName,
                dappUIDetails.longName,
                dappUIDetails.subtitle,
                dappUIDetails.buttonColor,
                dappUIDetails.iconLink,
                dappUIDetails.connectLink,
                dappUIDetails.isSponsored,
                dappUIDetails.sponsoredOrder
            )
        }
}