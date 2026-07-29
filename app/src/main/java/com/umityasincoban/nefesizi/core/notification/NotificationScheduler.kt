package com.umityasincoban.nefesizi.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.umityasincoban.nefesizi.MainActivity
import com.umityasincoban.nefesizi.R
import com.umityasincoban.nefesizi.core.data.NotificationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import com.umityasincoban.nefesizi.core.domain.nextDailyReminderDelay
import com.umityasincoban.nefesizi.core.domain.nextWeeklyReminderDelay
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun sync(preferences: NotificationPreferences) {
        if (preferences.eveningEnabled) {
            enqueuePeriodic(
                UNIQUE_EVENING,
                Duration.ofDays(1),
                nextDailyDelay(preferences.eveningTime),
                "Akşam özeti",
                "Bugünün kayıtlarına sakin bir göz atmak ister misin?",
            )
        } else {
            workManager.cancelUniqueWork(UNIQUE_EVENING)
        }
        if (preferences.weeklyEnabled) {
            enqueuePeriodic(
                UNIQUE_WEEKLY,
                Duration.ofDays(7),
                nextWeeklyDelay(),
                "Haftalık özetin hazır",
                "Bu haftaki kayıt ritmini ve maliyetini inceleyebilirsin.",
            )
        } else {
            workManager.cancelUniqueWork(UNIQUE_WEEKLY)
        }
        if (preferences.inactivityEnabled) {
            scheduleInactivity(preferences.inactivityDays)
        } else {
            workManager.cancelUniqueWork(UNIQUE_INACTIVITY)
        }
    }

    fun scheduleInactivity(days: Int) {
        val request = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(days.toLong(), TimeUnit.DAYS)
            .setInputData(
                messageData(
                    "Kayıt hatırlatıcısı",
                    "Bir süredir kayıt eklemedin. İstersen günlüğüne kaldığın yerden devam edebilirsin.",
                ),
            )
            .build()
        workManager.enqueueUniqueWork(UNIQUE_INACTIVITY, ExistingWorkPolicy.REPLACE, request)
    }

    private fun enqueuePeriodic(
        name: String,
        repeat: Duration,
        initial: Duration,
        title: String,
        message: String,
    ) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            repeat.toMinutes(),
            TimeUnit.MINUTES,
        )
            .setInitialDelay(initial)
            .setInputData(messageData(title, message))
            .build()
        workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun nextDailyDelay(value: String): Duration {
        val time = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.of(21, 0))
        val now = LocalDateTime.now(clock)
        return nextDailyReminderDelay(now, time)
    }

    private fun nextWeeklyDelay(): Duration {
        val now = LocalDateTime.now(clock)
        return nextWeeklyReminderDelay(
            now,
            java.time.DayOfWeek.SUNDAY,
            LocalTime.of(19, 0),
        )
    }

    private fun messageData(title: String, message: String) = Data.Builder()
        .putString(ReminderWorker.KEY_TITLE, title)
        .putString(ReminderWorker.KEY_MESSAGE, message)
        .build()

    companion object {
        const val UNIQUE_EVENING = "nefes_izi_evening_summary"
        const val UNIQUE_WEEKLY = "nefes_izi_weekly_summary"
        const val UNIQUE_INACTIVITY = "nefes_izi_inactivity"
    }
}

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Nefes İzi hatırlatıcıları", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(inputData.getString(KEY_TITLE).orEmpty())
            .setContentText(inputData.getString(KEY_MESSAGE).orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(inputData.getString(KEY_MESSAGE).orEmpty()))
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    Intent(applicationContext, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .build()
        manager.notify(inputData.getString(KEY_TITLE).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        private const val CHANNEL_ID = "nefes_izi_reminders"
    }
}
