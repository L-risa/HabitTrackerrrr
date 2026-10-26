package com.example.habittrackerrrr.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittrackerrrr.R
import com.example.habittrackerrrr.models.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class HabitsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: HabitListAdapter
    private val habitsList = mutableListOf<Habit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habits)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        val rvHabits = findViewById<RecyclerView>(R.id.rvHabits)
        rvHabits.layoutManager = LinearLayoutManager(this)
        adapter = HabitListAdapter(habitsList) { habit ->
            deleteHabit(habit)
        }
        rvHabits.adapter = adapter

        findViewById<Button>(R.id.btnAddHabit).setOnClickListener {
            val name = findViewById<EditText>(R.id.etNewHabit).text.toString().trim()
            if (name.isNotEmpty()) {
                addHabit(name)
                findViewById<EditText>(R.id.etNewHabit).text.clear()
            } else {
                Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show()
            }
        }

        loadHabits()
    }

    private fun addHabit(name: String) {
        val userId = auth.currentUser?.uid ?: return
        val habit = Habit(id = UUID.randomUUID().toString(), name = name)
        db.collection("users").document(userId).collection("habits")
            .document(habit.id).set(habit)
            .addOnSuccessListener { loadHabits() }
            .addOnFailureListener { Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show() }
    }

    private fun loadHabits() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("habits")
            .get()
            .addOnSuccessListener { documents ->
                habitsList.clear()
                for (doc in documents) {
                    doc.toObject(Habit::class.java)?.let { habit ->
                        habitsList.add(habit)
                    }
                }
                adapter.updateList(habitsList)
            }
    }

    private fun deleteHabit(habit: Habit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("habits")
            .document(habit.id).delete()
            .addOnSuccessListener { loadHabits() }
            .addOnFailureListener { Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show() }
    }

    inner class HabitListAdapter(
        private var habits: List<Habit>,
        private val onDelete: (Habit) -> Unit
    ) : RecyclerView.Adapter<HabitListAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val habit = habits[position]
            holder.text1.text = habit.name
            holder.text2.text = "Нажмите для удаления"
            holder.itemView.setOnClickListener { onDelete(habit) }
        }

        override fun getItemCount(): Int = habits.size

        fun updateList(newList: List<Habit>) {
            habits = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text1: TextView = itemView.findViewById(android.R.id.text1)
            val text2: TextView = itemView.findViewById(android.R.id.text2)
        }
    }
}