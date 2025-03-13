package com.trackasia.sample.navigation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.trackasia.navigation.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL
import com.trackasia.navigation.android.navigation.v5.navigation.notification.NavigationNotification
import com.trackasia.navigation.android.navigation.v5.routeprogress.RouteProgress
import com.trackasia.sample.R

class CustomNavigationNotification(private val applicationContext: Context) :
    NavigationNotification {

    companion object {
        private const val CUSTOM_NOTIFICATION_ID = 91234821
        private const val STOP_NAVIGATION_ACTION = "stop_navigation_action"
    }

    private val customNotificationBuilder: NotificationCompat.Builder
    private val customNotification: Notification
    private val notificationManager: NotificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var stopNavigationReceiver: BroadcastReceiver? = null
    private var numberOfUpdates = 0

    init {

        customNotificationBuilder =
            NotificationCompat.Builder(applicationContext, NAVIGATION_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_navigation_24)
                .setContentTitle("Custom Navigation Notification")
                .setContentText("Display your own content here!")
                .setContentIntent(createPendingStopIntent(applicationContext))

        customNotification = customNotificationBuilder.build()
    }

    override fun getNotification(): Notification {
        return customNotification
    }

    override fun getNotificationId(): Int {
        return CUSTOM_NOTIFICATION_ID
    }

    override fun updateNotification(routeProgress: RouteProgress) {
        // Update the builder with a new number of updates
        customNotificationBuilder.setContentText("Number of updates: $numberOfUpdates")
        notificationManager.notify(CUSTOM_NOTIFICATION_ID, customNotificationBuilder.build())
        numberOfUpdates++
    }

    override fun onNavigationStopped(context: Context) {
        stopNavigationReceiver?.let {
            context.unregisterReceiver(it)
        }
        notificationManager.cancel(CUSTOM_NOTIFICATION_ID)
    }

    fun register(stopNavigationReceiver: BroadcastReceiver, applicationContext: Context) {
        this.stopNavigationReceiver = stopNavigationReceiver
        ContextCompat.registerReceiver(
            applicationContext,
            stopNavigationReceiver,
            IntentFilter(STOP_NAVIGATION_ACTION),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun createPendingStopIntent(context: Context): PendingIntent {
        val stopNavigationIntent = Intent(STOP_NAVIGATION_ACTION)
        return PendingIntent.getBroadcast(
            context,
            0,
            stopNavigationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(context: Context) {
        val chan = NotificationChannel(
            NAVIGATION_NOTIFICATION_CHANNEL,
            "CustomNavigationNotification",
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }
}
