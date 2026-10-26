package com.example.habittrackerrrr.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.habittrackerrrr.R
import com.example.habittrackerrrr.models.Habit

class ProgressAdapter(
    private val progressMap: MutableMap<String, Boolean>,
    private val onCheckChange: (Habit, Boolean) -> Unit
) : ListAdapter<Habit, ProgressAdapter.ProgressViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_progress, parent, false)
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        val habit = getItem(position)
        val isChecked = progressMap[habit.id] ?: false
        holder.bind(habit, isChecked)
    }

    fun updateProgressMap(newMap: MutableMap<String, Boolean>) {
        progressMap.clear()
        progressMap.putAll(newMap)
        notifyDataSetChanged()
    }

    inner class ProgressViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        private val checkBox: CheckBox = itemView.findViewById(R.id.cbCompleted)

        fun bind(habit: Habit, isChecked: Boolean) {
            tvHabitName.text = habit.name
            checkBox.isChecked = isChecked
            checkBox.setOnCheckedChangeListener(null)
            checkBox.setOnCheckedChangeListener { _, checked ->
                onCheckChange(habit, checked)
            }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<Habit>() {
        override fun areItemsTheSame(oldItem: Habit, newItem: Habit): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Habit, newItem: Habit): Boolean =
            oldItem == newItem
    }
}