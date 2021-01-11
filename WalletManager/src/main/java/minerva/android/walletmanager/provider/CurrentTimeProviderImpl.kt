package minerva.android.walletmanager.provider

class CurrentTimeProviderImpl : CurrentTimeProvider {
    override fun currentTimeMills(): Long = System.currentTimeMillis()
}