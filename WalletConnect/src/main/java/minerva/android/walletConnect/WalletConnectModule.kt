package minerva.android.walletConnect

import androidx.room.Room
import minerva.android.walletConnect.database.WalletConnectDatabase
import minerva.android.walletConnect.providers.OkHttpProvider
import minerva.android.walletConnect.repository.WalletConnectRepository
import minerva.android.walletConnect.repository.WalletConnectRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val walletConnectModules = module {
    factory(named(QUALIFIER)) { OkHttpProvider.okHttpClient }
    factory<WalletConnectRepository> { WalletConnectRepositoryImpl(get(named(QUALIFIER)), get()) }
    single {
        Room.databaseBuilder(
            androidContext(),
            WalletConnectDatabase::class.java,
            "dapp_sessions_database"
        ).fallbackToDestructiveMigration().build()
    }
}

private const val QUALIFIER = "WalletConnect"