package minerva.android.app

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import io.fabric.sdk.android.Fabric
import io.reactivex.plugins.RxJavaPlugins
import minerva.android.BuildConfig
import minerva.android.di.createAppModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import coil.ImageLoader
import coil.Coil
import coil.util.CoilUtils
import coil.decode.SvgDecoder
import okhttp3.OkHttpClient


class MinervaApp : Application() {

    private val viewModel: AppViewModel by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MinervaApp)
            modules(createAppModule())
        }
        initializeCrashlytics()
        initializeTimber()
        initializeCoil()
        RxJavaPlugins.setErrorHandler { Timber.e(it) }
        viewModel.checkWalletConfigInitialization()
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

    private fun initializeCoil(){
        val imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(CoilUtils.createDefaultCache(this))
                    .build()
            }
            .componentRegistry { add(SvgDecoder(this@MinervaApp)) }
            .build()
        Coil.setImageLoader(imageLoader)
    }
}