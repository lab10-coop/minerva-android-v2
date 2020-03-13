package minerva.android.walletmanager.manager

import io.reactivex.Completable
import minerva.android.blockchainprovider.repository.contract.SmartContractRepository
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.mappers.TransactionMapper

class SmartContractManagerImpl(private val smartContractRepository: SmartContractRepository) : SmartContractManager {

    override fun createSafeAccount(value: Value) =
        smartContractRepository.deployGnosisSafeContract(value.privateKey, value.address, value.network)

    override fun transferNativeCoin(network: String, transaction: Transaction): Completable =
        smartContractRepository.transferNativeCoin(network, TransactionMapper.map(transaction))
}