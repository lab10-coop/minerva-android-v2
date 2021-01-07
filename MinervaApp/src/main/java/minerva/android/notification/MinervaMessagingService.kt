package minerva.android.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import minerva.android.R
import minerva.android.main.MainActivity
import minerva.android.main.MainActivity.Companion.JWT

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MinervaMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        showNotification(remoteMessage)
    }

    private fun showNotification(remoteMessage: RemoteMessage) {
        createNotificationChannel()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, getNotificationBuilder(remoteMessage.data[KEY]).build())
    }

    private fun getNotificationBuilder(jwt: String?): NotificationCompat.Builder =
        NotificationCompat.Builder(this, applicationContext.getString(R.string.channel_id))
            .setSmallIcon(R.drawable.ic_minerva_icon)
            .setContentTitle(getString(R.string.login_notification_title))
            .setContentText(getString(R.string.login_request_message))
            .setAutoCancel(true)
            .setContentIntent(getPendingIntent(jwt))

    private fun getPendingIntent(jwt: String?): PendingIntent =
        PendingIntent.getActivity(
            this,
            REQUEST_CODE,
            getIntent(jwt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
        )

    private fun getIntent(jwt: String?) =
        Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(JWT, jwt)
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                applicationContext.getString(R.string.channel_id),
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_NAME = "MinervaLogin"
        private const val REQUEST_CODE = 33
        private const val KEY = "disclosureRequest"
    }
}