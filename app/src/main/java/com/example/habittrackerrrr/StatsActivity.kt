package com.example.habittrackerrrr.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittrackerrrr.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvOverallProgress: TextView
    private lateinit var tvTotalCompleted: TextView
    private lateinit var tvBestStreak: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: MonthlyStatsAdapter
    private val monthlyStatsList = mutableListOf<MonthlyStats>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvOverallProgress = findViewById(R.id.tvOverallProgress)
        tvTotalCompleted = findViewById(R.id.tvTotalCompleted)
        tvBestStreak = findViewById(R.id.tvBestStreak)
        progressBar = findViewById(R.id.progressBar)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        val rvMonthlyStats = findViewById<RecyclerView>(R.id.rvMonthlyStats)
        rvMonthlyStats.layoutManager = LinearLayoutManager(this)
        adapter = MonthlyStatsAdapter(monthlyStatsList)
        rvMonthlyStats.adapter = adapter

        loadStatistics()
    }

    private fun loadStatistics() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = ProgressBar.VISIBLE

        db.collection("users").document(userId).collection("completions")
            .get()
            .addOnSuccessListener { documents ->

                val completions = mutableListOf<CompletionData>()
                for (doc in documents) {
                    val id = doc.id
                    val completed = doc.getBoolean("completed") ?: false
                    val parts = id.split("_")
                    if (parts.size == 2) {
                        completions.add(CompletionData(
                            habitId = parts[0],
                            date = parts[1],
                            completed = completed
                        ))
                    }
                }

                calculateOverallStats(completions)
                calculateMonthlyStats(completions)
                progressBar.visibility = ProgressBar.GONE
            }
            .addOnFailureListener { e ->
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateOverallStats(completions: List<CompletionData>) {
        val total = completions.size
        val completed = completions.count { it.completed }
        val percentage = if (total > 0) (completed * 100 / total) else 0

        tvOverallProgress.text = percentage.toString() + "%"
        tvTotalCompleted.text = completed.toString()

        val datesWithCompletions = completions
            .filter { it.completed }
            .map { it.date }
            .distinct()
            .sorted()

        var currentStreak = 0
        var bestStreak = 0

        for (i in datesWithCompletions.indices) {
            if (i == 0) {
                currentStreak = 1
            } else {
                val diff = dateDiff(datesWithCompletions[i - 1], datesWithCompletions[i])
                if (diff == 1) {
                    currentStreak++
                } else {
                    currentStreak = 1
                }
            }
            if (currentStreak > bestStreak) bestStreak = currentStreak
        }

        tvBestStreak.text = bestStreak.toString()
    }

    private fun calculateMonthlyStats(completions: List<CompletionData>) {
        val monthlyMap = mutableMapOf<String, MutableList<CompletionData>>()

        for (completion in completions) {
            if (completion.date.length >= 7) {
                val monthKey = completion.date.substring(0, 7)
                if (!monthlyMap.containsKey(monthKey)) {
                    monthlyMap[monthKey] = mutableListOf()
                }
                monthlyMap[monthKey]?.add(completion)
            }
        }

        monthlyStatsList.clear()
        for ((month, data) in monthlyMap) {
            val total = data.size
            val completed = data.count { it.completed }
            val percentage = if (total > 0) (completed * 100 / total) else 0
            monthlyStatsList.add(MonthlyStats(month, percentage, completed, total))
        }

        monthlyStatsList.sortByDescending { it.month }
        adapter.updateList(monthlyStatsList)
    }

    private fun dateDiff(date1: String, date2: String): Int {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val d1 = format.parse(date1)
            val d2 = format.parse(date2)
            val diff = d2?.time?.minus(d1?.time ?: 0) ?: 0
            (diff / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    data class CompletionData(val habitId: String, val date: String, val completed: Boolean)
    data class MonthlyStats(val month: String, val percentage: Int, val completed: Int, val total: Int)

    inner class MonthlyStatsAdapter(
        private var stats: List<MonthlyStats>
    ) : RecyclerView.Adapter<MonthlyStatsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val stat = stats[position]
            val monthName = formatMonth(stat.month)
            holder.text1.text = monthName + " — " + stat.percentage + "%"
            holder.text2.text = "Выполнено: " + stat.completed + " из " + stat.total + " дней"
        }

        override fun getItemCount(): Int = stats.size

        fun updateList(newStats: List<MonthlyStats>) {
            stats = newStats
            notifyDataSetChanged()
        }

        private fun formatMonth(month: String): String {
            return try {
                val parts = month.split("-")
                if (parts.size == 2) {
                    val year = parts[0]
                    val monthNum = parts[1].toInt()
                    val monthName = when (monthNum) {
                        1 -> "Январь"
                        2 -> "Февраль"
                        3 -> "Март"
                        4 -> "Апрель"
                        5 -> "Май"
                        6 -> "Июнь"
                        7 -> "Июль"
                        8 -> "Август"
                        9 -> "Сентябрь"
                        10 -> "Октябрь"
                        11 -> "Ноябрь"
                        12 -> "Декабрь"
                        else -> month
                    }
                    "$monthName $year"
                } else {
                    month
                }
            } catch (e: Exception) {
                month
            }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text1: TextView = itemView.findViewById(android.R.id.text1)
            val text2: TextView = itemView.findViewById(android.R.id.text2)
        }
    }
}