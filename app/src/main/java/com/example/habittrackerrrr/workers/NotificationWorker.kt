package com.example.habittrackerrrr.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        // Получаем название привычки из данных
        val habitName = inputData.getString("habit_name") ?: return Result.failure()

        // Показываем уведомление
        sendNotification(habitName)

        return Result.success()
    }

    private fun sendNotification(habitName: String) {
        // Создаём канал для уведомлений (нужно для Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "habit_reminders",
                "Напоминания о привычках",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о выполнении привычек"
                enableVibration(true)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Создаём само уведомление
        val notification = NotificationCompat.Builder(applicationContext, "habit_reminders")
            .setSmallIcon(android.R.drawable.ic_menu_edit)  // Иконка
            .setContentTitle("🔔 Напоминание о привычке")
            .setContentText("Не забудьте выполнить: $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Показываем уведомление
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(habitName.hashCode(), notification)
    }
}