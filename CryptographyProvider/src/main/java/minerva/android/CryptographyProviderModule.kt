package minerva.android

import minerva.android.repository.CryptographyRepository
import minerva.android.repository.CryptographyRepositoryImpl
import org.koin.dsl.module

fun createCryptographyModules() = module {
    single { CryptographyRepositoryImpl(get()) as CryptographyRepository }
}