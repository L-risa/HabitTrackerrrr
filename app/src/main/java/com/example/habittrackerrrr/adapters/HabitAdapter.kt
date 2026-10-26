package com.example.habittrackerrrr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.habittrackerrrr.R
import com.example.habittrackerrrr.models.Habit
import com.google.android.material.button.MaterialButton

class HabitAdapter(
    private val onDeleteClick: (Habit) -> Unit,
    private val onCheckChange: (Habit, Boolean) -> Unit,
    private val onNotificationClick: (Habit) -> Unit,
    private val onEditClick: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)
        holder.bind(habit)
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        private val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        private val btnNotification: ImageButton = itemView.findViewById(R.id.btnNotification)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)

        fun bind(habit: Habit) {
            tvHabitName.text = habit.name
            cbCompleted.isChecked = habit.isCompleted

            if (habit.notificationEnabled) {
                btnNotification.setImageResource(android.R.drawable.ic_lock_idle_alarm)
                btnNotification.setColorFilter(ContextCompat.getColor(itemView.context, R.color.primary_light))
            } else {
                btnNotification.setImageResource(android.R.drawable.ic_lock_idle_alarm)
                btnNotification.setColorFilter(ContextCompat.getColor(itemView.context, R.color.text_secondary_light))
            }

            cbCompleted.setOnCheckedChangeListener(null)
            cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onCheckChange(habit, isChecked)
            }

            btnDelete.setOnClickListener { onDeleteClick(habit) }
            btnNotification.setOnClickListener { onNotificationClick(habit) }
            btnEdit.setOnClickListener { onEditClick(habit) }

            itemView.setOnLongClickListener {
                onEditClick(habit)
                true
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