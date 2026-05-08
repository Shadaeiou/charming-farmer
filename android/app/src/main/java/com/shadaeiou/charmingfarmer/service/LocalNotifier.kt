package com.shadaeiou.charmingfarmer.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.shadaeiou.charmingfarmer.FarmerApp
import com.shadaeiou.charmingfarmer.MainActivity
import com.shadaeiou.charmingfarmer.R
import com.shadaeiou.charmingfarmer.data.NotificationSettings
import java.util.concurrent.TimeUnit

/**
 * Schedules and fires local notifications for in-game events that
 * complete on a known timer (crop growth, kiln runs, brew batches,
 * cooking, transport trips). Backed by WorkManager so notifications
 * fire even when the app is closed or the device has rebooted.
 *
 * Each scheduled work is identified by a unique tag (e.g.
 * "crop_$plotIdx", "trip_$tripId"). Re-scheduling against the same
 * tag REPLACEs the old worker, so a player who tills and replants
 * doesn't end up with stale notifications.
 *
 * The Worker honors [NotificationSettings] at fire time, so toggling
 * the master switch or a per-channel toggle silences scheduled work
 * the next time it fires without us having to cancel it.
 */
object LocalNotifier {

    private const val ALL_TAG = "cf_local_notif"
    const val KEY_CHANNEL = "channel"
    const val KEY_TITLE = "title"
    const val KEY_BODY = "body"

    fun schedule(
        context: Context,
        channel: NotificationSettings.Channel,
        delayMs: Long,
        title: String,
        body: String,
        uniqueTag: String,
    ) {
        if (delayMs <= 0L) {
            // Already due — fire immediately on a background thread via
            // the worker, with no delay. WorkManager handles dispatch.
        }
        val safeDelay = delayMs.coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(KEY_CHANNEL, channel.name)
            .putString(KEY_TITLE, title)
            .putString(KEY_BODY, body)
            .build()
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(safeDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(ALL_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueTag,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, uniqueTag: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueTag)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(ALL_TAG)
    }

    /** Fire a notification immediately — used by the Settings test
     *  button so the player can confirm permissions and channel routing
     *  without waiting hours for a real event. */
    fun showNow(
        context: Context,
        channel: NotificationSettings.Channel,
        title: String,
        body: String,
    ) {
        deliver(context, channel, title, body, ignoreSettings = true)
    }

    internal fun deliver(
        context: Context,
        channel: NotificationSettings.Channel,
        title: String,
        body: String,
        ignoreSettings: Boolean = false,
    ) {
        NotificationSettings.init(context.applicationContext)
        if (!ignoreSettings && !NotificationSettings.shouldNotify(channel)) return

        // Android 13+ requires runtime POST_NOTIFICATIONS permission.
        // If the user denied it, NotificationManagerCompat silently
        // drops the call — better to bail explicitly so we don't waste
        // cycles building a notification that goes nowhere.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            channel.ordinal,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, FarmerApp.CHANNEL_GAME_EVENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Per-event id so different notifications don't collapse into
        // each other. Time-based id is stable enough — we don't need
        // dedup beyond what unique-work provides at schedule time.
        val notifId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }
}

/**
 * WorkManager Worker that fires a notification scheduled by
 * [LocalNotifier]. Reads the channel, title, and body from inputData;
 * delivery is gated by [NotificationSettings] so toggling a channel
 * off silences pending work without us needing to cancel it.
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val channelName = inputData.getString(LocalNotifier.KEY_CHANNEL)
            ?: return Result.success()
        val channel = runCatching {
            NotificationSettings.Channel.valueOf(channelName)
        }.getOrNull() ?: return Result.success()

        val title = inputData.getString(LocalNotifier.KEY_TITLE) ?: "Charming Farmer"
        val body = inputData.getString(LocalNotifier.KEY_BODY) ?: ""
        LocalNotifier.deliver(applicationContext, channel, title, body)
        return Result.success()
    }
}
