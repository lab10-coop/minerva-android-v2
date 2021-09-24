package minerva.android.blockchainprovider.repository.superToken

import io.reactivex.Flowable
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.blockchainprovider.smartContracts.ConstantFlowAgreementV1
import minerva.android.kotlinUtils.map.value
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import java.math.BigInteger

class SuperTokenRepositoryImpl(
    private val web3j: Map<Int, Web3j>,
    private val gasPrices: Map<Int, BigInteger>
) : SuperTokenRepository {

    private fun loadCFA(cfaAddress: String, chainId: Int, privateKey: String): ConstantFlowAgreementV1 =
        ConstantFlowAgreementV1.load(
            cfaAddress, web3j.value(chainId),
            Credentials.create(privateKey),
            ContractGasProvider(gasPrices.value(chainId), Operation.TRANSFER_ERC20.gasLimit)
        )

    override fun getNetFlow(
        cfaAddress: String,
        chainId: Int,
        privateKey: String,
        tokenAddress: String,
        accountAddress: String
    ): Flowable<BigInteger> =
        loadCFA(cfaAddress, chainId, privateKey)
            .getNetFlow(tokenAddress, accountAddress)
            .flowable()
}