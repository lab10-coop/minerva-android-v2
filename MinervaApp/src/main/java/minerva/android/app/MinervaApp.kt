package minerva.android.app

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import io.fabric.sdk.android.Fabric
import io.reactivex.plugins.RxJavaPlugins
import minerva.android.BuildConfig
import minerva.android.di.createAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class MinervaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MinervaApp)
            modules(createAppModule())
        }
        initializeCrashlytics()
        initializeTimber()
        RxJavaPlugins.setErrorHandler { Timber.e(it) }
    }

    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }

    private fun initializeCrashlytics() {
        val crashlyticsCore = CrashlyticsCore.Builder()
            .disabled(BuildConfig.DEBUG)
            .build()
        Fabric.with(this, Crashlytics.Builder().core(crashlyticsCore).build())
    }
}