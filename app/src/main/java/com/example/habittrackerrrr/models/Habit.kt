package com.example.habittrackerrrr.models

data class Habit(
    val id: String = "",
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var isCompleted: Boolean = false,
    val notificationHour: Int = -1,
    val notificationMinute: Int = -1,
    val notificationEnabled: Boolean = false
)