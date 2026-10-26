package com.example.habittrackerrrr.utils

import android.content.Context
import androidx.work.*
import com.example.habittrackerrrr.models.Habit
import com.example.habittrackerrrr.workers.NotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleNotification(habit: Habit) {
        if (!habit.notificationEnabled || habit.notificationHour == -1) {
            cancelNotification(habit.id)
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, habit.notificationHour)
            set(Calendar.MINUTE, habit.notificationMinute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val inputData = workDataOf(
            "habit_name" to habit.name,
            "habit_id" to habit.id
        )

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "habit_notification_${habit.id}",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelNotification(habitId: String) {
        workManager.cancelUniqueWork("habit_notification_$habitId")
    }
}