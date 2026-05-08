package com.shadaeiou.charmingfarmer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.content.getSystemService
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class FarmerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createUpdatesNotificationChannel()
        createGameEventsChannel()
        initFirebaseIfConfigured()
    }

    private fun createUpdatesNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_UPDATES,
            "App updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications about new app versions available to install."
        }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun createGameEventsChannel() {
        val channel = NotificationChannel(
            CHANNEL_GAME_EVENTS,
            "Farm events",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Crops ready, crafting finished, transport arrived."
        }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun initFirebaseIfConfigured() {
        val app = try {
            FirebaseApp.initializeApp(this)
        } catch (t: Throwable) {
            Log.i(TAG, "Firebase not initialized: ${t.message}")
            return
        }
        if (app == null) {
            Log.i(TAG, "Firebase not initialized: placeholder google-services.json detected")
            return
        }
        FirebaseMessaging.getInstance().subscribeToTopic(UPDATE_TOPIC)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Subscribed to FCM topic '$UPDATE_TOPIC'")
                } else {
                    Log.w(TAG, "Failed to subscribe to FCM topic '$UPDATE_TOPIC'", task.exception)
                }
            }
    }

    companion object {
        const val CHANNEL_UPDATES = "app_updates"
        const val CHANNEL_GAME_EVENTS = "game_events"
        const val UPDATE_TOPIC = "app-updates"
        private const val TAG = "FarmerApp"
    }
}
