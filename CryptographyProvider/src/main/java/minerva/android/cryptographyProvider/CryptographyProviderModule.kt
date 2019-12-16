package minerva.android.cryptographyProvider

import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepositoryImpl
import org.koin.dsl.module

fun createCryptographyModules() = module {
    single { CryptographyRepositoryImpl(get()) as CryptographyRepository }
}