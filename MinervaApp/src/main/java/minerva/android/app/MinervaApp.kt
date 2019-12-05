package minerva.android.app

import android.app.Application
import minerva.android.di.createAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MinervaApp() : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MinervaApp)
            modules(createAppModule())
        }
    }

}