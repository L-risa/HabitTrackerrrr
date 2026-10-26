package com.example.habittrackerrrr.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.habittrackerrrr.LoginActivity
import com.example.habittrackerrrr.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupButtons()
        loadUserData()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnResetPassword).setOnClickListener { resetPassword() }
        findViewById<Button>(R.id.btnDeleteAccount).setOnClickListener { showDeleteDialog() }

    }

    private fun loadUserData() {
        val user = auth.currentUser
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvUserId = findViewById<TextView>(R.id.tvUserId)
        val tvMemberSince = findViewById<TextView>(R.id.tvMemberSince)

        tvEmail.text = user?.email ?: "Email не указан"
        tvUserId.text = "ID: ${user?.uid?.take(8)}..."

        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    val createdAt = doc.getLong("createdAt")
                    if (createdAt != null) {
                        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            .format(Date(createdAt))
                        tvMemberSince.text = "Присоединился: $date"
                    }
                }
        }
    }

    private fun resetPassword() {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "Email не указан", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Письмо для сброса пароля отправлено на $email", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удаление аккаунта")
            .setMessage("Все данные будут потеряны. Вы уверены?")
            .setPositiveButton("Да, удалить") { _, _ -> deleteAccount() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid).delete()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Аккаунт удалён", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
            }
    }
}