package minerva.android.blockchainprovider.repository.ens

import io.reactivex.Single
import org.web3j.ens.EnsResolver

class ENSRepositoryImpl(private val ensResolver: EnsResolver) : ENSRepository {
    override fun reverseResolveENS(ensAddress: String): Single<String> =
        Single.just(ensAddress).map { address -> ensResolver.reverseResolve(address) }

    override fun resolveENS(ensName: String): Single<String> =
        if (ensName.contains(DOT)) {
            Single.just(ensName).map { name -> ensResolver.resolve(name) }
                .onErrorReturnItem(ensName)
        } else {
            Single.just(ensName)
        }

    companion object {
        private const val DOT = "."
    }
}