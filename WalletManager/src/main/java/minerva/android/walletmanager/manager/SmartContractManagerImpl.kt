package minerva.android.walletmanager.manager

import minerva.android.blockchainprovider.repository.contract.SmartContractRepository
import minerva.android.walletmanager.model.Value

class SmartContractManagerImpl(private val smartContractRepository: SmartContractRepository) : SmartContractManager {

    override fun createSafeAccount(value: Value) =
        smartContractRepository.deployGnosisSafeContract(value.privateKey, value.address, value.network)
}