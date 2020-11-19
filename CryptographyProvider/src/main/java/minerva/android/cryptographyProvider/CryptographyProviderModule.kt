package minerva.android.cryptographyProvider

import me.uport.sdk.jwt.JWTTools
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepositoryImpl
import org.koin.dsl.module

fun createCryptographyModules() = module {
    single<CryptographyRepository> { CryptographyRepositoryImpl(get()) }
    single { JWTTools() }
}