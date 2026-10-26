package com.example.habittrackerrrr

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittrackerrrr.activities.*
import com.example.habittrackerrrr.adapters.HabitAdapter
import com.example.habittrackerrrr.models.Habit
import com.example.habittrackerrrr.utils.NotificationScheduler
import com.example.habittrackerrrr.utils.TimePickerDialogFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var habitAdapter: HabitAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var tvProgress: TextView
    private lateinit var tvCompleted: TextView
    private lateinit var tvStreak: TextView
    private lateinit var notificationScheduler: NotificationScheduler

    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        notificationScheduler = NotificationScheduler(this)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        requestNotificationPermission()
        initViews()
        loadHabits()
        setupListeners()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun initViews() {
        tvProgress = findViewById(R.id.tvProgress)
        tvCompleted = findViewById(R.id.tvCompleted)
        tvStreak = findViewById(R.id.tvStreak)

        val rvHabits = findViewById<RecyclerView>(R.id.rvHabits)
        rvHabits.layoutManager = LinearLayoutManager(this)

        habitAdapter = HabitAdapter(
            onDeleteClick = { habit -> deleteHabit(habit) },
            onCheckChange = { habit, isChecked -> updateHabitCompletion(habit, isChecked) },
            onNotificationClick = { habit -> showTimePickerDialog(habit) },
            onEditClick = { habit -> editHabit(habit) }
        )
        rvHabits.adapter = habitAdapter

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvWelcome.text = auth.currentUser?.email ?: "Пользователь"

        tvProgress.text = "0%"
        tvCompleted.text = "0"
        tvStreak.text = "0"
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<FloatingActionButton>(R.id.fabAddHabit).setOnClickListener {
            showAddHabitDialog()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadHabits()
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun showAddHabitDialog() {
        val input = EditText(this)
        input.hint = "Название привычки"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("➕ Добавить привычку")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    addHabit(name)
                } else {
                    Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addHabit(name: String) {
        if (isUpdating) return

        val userId = auth.currentUser?.uid ?: return

        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = name,
            notificationHour = -1,
            notificationMinute = -1,
            notificationEnabled = false,
            isCompleted = false
        )

        isUpdating = true

        db.collection("users").document(userId).collection("habits")
            .document(habit.id).set(habit)
            .addOnSuccessListener {
                loadHabits()
                Toast.makeText(this, "✅ Привычка добавлена", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка добавления: ${e.message}", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
    }

    private fun loadHabits() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("habits")
            .get()
            .addOnSuccessListener { documents ->
                val habitsList = mutableListOf<Habit>()
                for (doc in documents) {
                    val habit = doc.toObject(Habit::class.java)
                    habitsList.add(habit)
                }
                // ListAdapter сам обработает обновление
                habitAdapter.submitList(habitsList)
                updateStatistics()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteHabit(habit: Habit) {
        if (isUpdating) return

        val userId = auth.currentUser?.uid ?: return

        notificationScheduler.cancelNotification(habit.id)

        isUpdating = true

        db.collection("users").document(userId).collection("habits")
            .document(habit.id).delete()
            .addOnSuccessListener {
                loadHabits()
                Toast.makeText(this, "🗑 Привычка удалена", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
    }

    private fun editHabit(habit: Habit) {
        val input = EditText(this)
        input.hint = "Новое название"
        input.setText(habit.name)
        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle("✏️ Редактировать привычку")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateHabitName(habit, newName)
                } else {
                    Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateHabitName(habit: Habit, newName: String) {
        if (isUpdating) return

        val userId = auth.currentUser?.uid ?: return
        val updatedHabit = habit.copy(name = newName)

        isUpdating = true

        db.collection("users").document(userId).collection("habits")
            .document(habit.id).set(updatedHabit)
            .addOnSuccessListener {
                loadHabits()
                Toast.makeText(this, "✅ Привычка обновлена", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
    }

    private fun updateHabitCompletion(habit: Habit, isChecked: Boolean) {
        if (isUpdating) return

        val userId = auth.currentUser?.uid ?: return
        val dateStr = dateFormat.format(Date())

        isUpdating = true

        val completionData = hashMapOf(
            "completed" to isChecked,
            "habitId" to habit.id,
            "habitName" to habit.name,
            "date" to dateStr
        )

        db.collection("users").document(userId).collection("completions")
            .document("${habit.id}_$dateStr")
            .set(completionData)
            .addOnSuccessListener {
                updateStatistics()
                if (isChecked) {
                    Toast.makeText(this, "🎉 Отлично! +1 балл", Toast.LENGTH_SHORT).show()
                }
                isUpdating = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
    }

    private fun updateStatistics() {
        val userId = auth.currentUser?.uid ?: return
        val dateStr = dateFormat.format(Date())

        db.collection("users").document(userId).collection("habits")
            .get()
            .addOnSuccessListener { habits ->
                val totalHabits = habits.size()

                if (totalHabits == 0) {
                    tvProgress.text = "0%"
                    tvCompleted.text = "0"
                    tvStreak.text = "0"
                    return@addOnSuccessListener
                }

                var completedToday = 0
                var pendingRequests = totalHabits

                for (habit in habits) {
                    val habitId = habit.id
                    db.collection("users").document(userId).collection("completions")
                        .document("${habitId}_$dateStr")
                        .get()
                        .addOnSuccessListener { doc ->
                            if (doc.getBoolean("completed") == true) {
                                completedToday++
                            }
                            pendingRequests--
                            if (pendingRequests == 0) {
                                val percentage = (completedToday * 100 / totalHabits)
                                tvProgress.text = "$percentage%"
                                tvCompleted.text = completedToday.toString()
                                tvStreak.text = completedToday.toString()
                            }
                        }
                        .addOnFailureListener {
                            pendingRequests--
                            if (pendingRequests == 0) {
                                val percentage = (completedToday * 100 / totalHabits)
                                tvProgress.text = "$percentage%"
                                tvCompleted.text = completedToday.toString()
                                tvStreak.text = completedToday.toString()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка статистики: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTimePickerDialog(habit: Habit) {
        val dialog = TimePickerDialogFragment { hour, minute ->
            updateHabitNotification(habit, hour, minute)
        }
        dialog.show(supportFragmentManager, "time_picker")
    }

    private fun updateHabitNotification(habit: Habit, hour: Int, minute: Int) {
        if (isUpdating) return

        val userId = auth.currentUser?.uid ?: return

        val updatedHabit = habit.copy(
            notificationHour = hour,
            notificationMinute = minute,
            notificationEnabled = true
        )

        isUpdating = true

        db.collection("users").document(userId).collection("habits")
            .document(habit.id).set(updatedHabit)
            .addOnSuccessListener {
                loadHabits()
                notificationScheduler.scheduleNotification(updatedHabit)
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                Toast.makeText(this, "🔔 Напоминание установлено на $timeString", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                isUpdating = false
            }
    }
}