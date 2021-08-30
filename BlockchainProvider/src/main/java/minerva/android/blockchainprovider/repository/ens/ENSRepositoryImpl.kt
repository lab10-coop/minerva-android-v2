package minerva.android.blockchainprovider.repository.ens

import io.reactivex.Single
import org.web3j.crypto.Keys
import org.web3j.crypto.WalletUtils
import org.web3j.ens.EnsResolver
import java.util.*

class ENSRepositoryImpl(private val ensResolver: EnsResolver) : ENSRepository {

    override fun toChecksumAddress(address: String): String = Keys.toChecksumAddress(address)

    override fun isAddressValid(address: String): Boolean =
        WalletUtils.isValidAddress(address) &&
                (Keys.toChecksumAddress(address) == address || address.toLowerCase(Locale.ROOT) == address)

    override fun reverseResolveENS(ensAddress: String): Single<String> =
        Single.just(ensAddress).map { address -> ensResolver.reverseResolve(address) }

    override fun resolveENS(ensName: String): Single<String> =
        if (ensName.contains(DOT)) {
            Single.just(ensName).map { name -> ensResolver.resolve(name) }.onErrorReturnItem(ensName)
        } else {
            Single.just(ensName)
        }

    companion object {
        private const val DOT = "."
    }
}