package minerva.android.blockchainprovider.repository.ens

import io.reactivex.Single

interface ENSRepository {
    fun reverseResolveENS(ensAddress: String): Single<String>
    fun resolveENS(ensName: String): Single<String>
}