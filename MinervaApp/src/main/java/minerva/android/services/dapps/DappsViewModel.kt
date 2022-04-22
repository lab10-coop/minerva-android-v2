package minerva.android.services.dapps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.services.dapps.model.Dapp
import minerva.android.services.dapps.model.DappsWithCategories
import minerva.android.utils.MyHelper.l
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.dapps.DappUIDetails
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.repository.dapps.DappsRepository
import minerva.android.walletmanager.storage.LocalStorage

class DappsViewModel(
    private val dappsRepository: DappsRepository,
    private val localStorage: LocalStorage
) : BaseViewModel() {
    private val _dappsLiveData: MutableLiveData<DappsWithCategories> = MutableLiveData()
    val dappsLiveData: LiveData<DappsWithCategories> get() = _dappsLiveData

    private var dapps: List<Dapp> = emptyList()
    //property for store previous filter value
    private var prevFilterValue: Int = DappsFragment.allDappItemChainId

    /**
     * Get Filtered Networks - method which get list of filtered networks
     * @return List<Network> - instance of minerva.android.walletmanager.model.network.Network
     */
    fun getFilteredNetworks(): List<Network> {
        //get network chains of active dapps, for prevent showing network which dapps not use
        val idsOfActiveChains: MutableList<Int> = mutableListOf()
        //choosing unique network chain id
        dapps.forEach { dapp ->
            dapp.chainIds.forEach { chainId ->
                if (!idsOfActiveChains.contains(chainId)) idsOfActiveChains.add(chainId)
            }
        }

        val networks: MutableList<Network> = mutableListOf(
            //add default "All" filter item
            Network(
                name = DappsFragment.allDappItemName,
                chainId = DappsFragment.allDappItemChainId,
                isActive = true,
                testNet = true)
        )
        //sort by "test" network
        if (localStorage.areMainNetworksEnabled) NetworkManager.networks
            .filter { it.isActive && idsOfActiveChains.contains(it.chainId) }
            .forEach { networks.add(it) }
        else NetworkManager.networks
            .filter { it.isActive && it.testNet && idsOfActiveChains.contains(it.chainId) }
            .forEach { networks.add(it) }
        return networks
    }

    /**
     * Filter By Network Id - method which filters dapps by network id
     * @param chainId - Int - id of Network
     */
    fun filterByNetworkId(chainId: Int) {
        var filteredDapps: List<Dapp>
        if (chainId == DappsFragment.allDappItemChainId)
            filteredDapps = dapps
        else
            filteredDapps = dapps.filter { it.chainIds.contains(chainId) }

        _dappsLiveData.value = DappsWithCategories(
            favorite = filteredDapps.filter { it.isFavorite }.sortedBy { it.shortName.toUpperCase() },
            sponsored = filteredDapps.filter { !it.isFavorite && it.isSponsored }.sortedBy { it.sponsoredOrder },
            remaining = filteredDapps.filter { !it.isFavorite && !it.isSponsored }.sortedBy { it.shortName.toUpperCase() }
        )
    }

    fun updateFavoriteDapp(name: String) {
        if (dapps.find { it.shortName == name }?.isFavorite == true) {
            removeFavoriteDapp(name)
        } else {
            insertFavoriteDapp(name)
        }
    }

    private fun removeFavoriteDapp(name: String) {
        launchDisposable {
            dappsRepository.removeFavoriteDapp(name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateDapps() }
        }
    }

    private fun insertFavoriteDapp(name: String) {
        launchDisposable {
            dappsRepository.insertFavoriteDapp(name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateDapps() }
        }
    }

    private fun updateDapps() {
        launchDisposable {
            dappsRepository.getAllDappsDetailsFromDB()
                .zipWith(dappsRepository.getFavoriteDapps())
                .map { (dapps, favorites) -> dapps.mapToDappList(favorites) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { dapps -> handleDappResult(dapps) }
        }
    }

    fun getDapps() {
        launchDisposable {
            dappsRepository.getAllDappsDetails()
                .zipWith(dappsRepository.getFavoriteDapps())
                .map { (dapps, favorites) -> dapps.mapToDappList(favorites) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { dapps -> handleDappResult(dapps) }
        }
    }

    private fun handleDappResult(list: List<Dapp>) {
        this.dapps = list
        _dappsLiveData.value = DappsWithCategories(
            favorite = list.filter { it.isFavorite }.sortedBy { it.shortName.toUpperCase() },
            sponsored = list.filter { !it.isFavorite && it.isSponsored }.sortedBy { it.sponsoredOrder },
            remaining = list.filter { !it.isFavorite && !it.isSponsored }.sortedBy { it.shortName.toUpperCase() }
        )
    }

    private fun List<DappUIDetails>.mapToDappList(favorites: List<String>): List<Dapp> =
        map { dappUIDetails ->
            Dapp(
                dappUIDetails.shortName,
                dappUIDetails.longName,
                dappUIDetails.subtitle,
                dappUIDetails.buttonColor,
                dappUIDetails.iconLink,
                dappUIDetails.connectLink,
                dappUIDetails.isSponsored,
                dappUIDetails.sponsoredOrder,
                favorites.contains(dappUIDetails.shortName),
                dappUIDetails.chainIds
            )
        }
}
