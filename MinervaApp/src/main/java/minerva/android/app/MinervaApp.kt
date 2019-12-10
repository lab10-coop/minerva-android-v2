package minerva.android.app

import android.app.Application
import minerva.android.BuildConfig
import minerva.android.di.createAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class MinervaApp() : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MinervaApp)
            modules(createAppModule())
        }
        initializeTimber()
    }

    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            //TODO add Crash/Firebase logging when distribution staff will be approved
        }
    }
}