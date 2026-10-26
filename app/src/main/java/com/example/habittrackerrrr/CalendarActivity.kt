package com.example.habittrackerrrr.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittrackerrrr.R
import com.example.habittrackerrrr.models.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: CompletedHabitsAdapter
    private val completedHabitsList = mutableListOf<Habit>()
    private val allHabitsList = mutableListOf<Habit>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var tvNoHabits: TextView
    private lateinit var tvSelectedDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvNoHabits = findViewById(R.id.tvNoHabits)

        val rvCompletedHabits = findViewById<RecyclerView>(R.id.rvCompletedHabits)
        rvCompletedHabits.layoutManager = LinearLayoutManager(this)
        adapter = CompletedHabitsAdapter(completedHabitsList)
        rvCompletedHabits.adapter = adapter

        val calendarView = findViewById<CalendarView>(R.id.calendarView)

        loadAllHabits {
            val today = Date()
            tvSelectedDate.text = "📅 ${dateFormat.format(today)}"
            loadCompletedHabitsForDate(today)
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = GregorianCalendar(year, month, dayOfMonth).time
            tvSelectedDate.text = "📅 ${dateFormat.format(selectedDate)}"
            loadCompletedHabitsForDate(selectedDate)
        }
    }

    private fun loadAllHabits(onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("habits")
            .get()
            .addOnSuccessListener { documents ->
                allHabitsList.clear()
                for (doc in documents) {
                    val habit = doc.toObject(Habit::class.java)
                    allHabitsList.add(habit)
                }
                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки привычек", Toast.LENGTH_SHORT).show()
                onComplete()
            }
    }

    private fun loadCompletedHabitsForDate(date: Date) {
        val userId = auth.currentUser?.uid ?: return
        val dateStr = dateFormat.format(date)

        completedHabitsList.clear()
        adapter.updateList(completedHabitsList)

        if (allHabitsList.isEmpty()) {
            tvNoHabits.visibility = View.VISIBLE
            tvNoHabits.text = "Сначала добавьте привычки на главном экране"
            return
        }

        var pendingRequests = allHabitsList.size
        var hasResults = false

        for (habit in allHabitsList) {
            db.collection("users").document(userId).collection("completions")
                .document("${habit.id}_$dateStr")
                .get()
                .addOnSuccessListener { doc ->
                    val completed = doc.getBoolean("completed") ?: false
                    if (completed) {
                        completedHabitsList.add(habit)
                        hasResults = true
                    }

                    pendingRequests--
                    if (pendingRequests == 0) {
                        // Все запросы завершены, обновляем UI
                        adapter.updateList(completedHabitsList)

                        if (completedHabitsList.isEmpty()) {
                            tvNoHabits.visibility = View.VISIBLE
                            tvNoHabits.text = "Нет выполненных привычек за этот день"
                        } else {
                            tvNoHabits.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { e ->
                    pendingRequests--
                    if (pendingRequests == 0 && !hasResults) {
                        tvNoHabits.visibility = View.VISIBLE
                        tvNoHabits.text = "Нет выполненных привычек за этот день"
                    }
                }
        }
    }

    inner class CompletedHabitsAdapter(
        private var habits: List<Habit>
    ) : RecyclerView.Adapter<CompletedHabitsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val habit = habits[position]
            holder.textView.text = "✅ ${habit.name} — выполнено!"
        }

        override fun getItemCount(): Int = habits.size

        fun updateList(newList: List<Habit>) {
            habits = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(android.R.id.text1)
        }
    }
}