package com.example.cameraxapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.cardview.widget.CardView

class SignInActivity : AppCompatActivity() {

    private lateinit var btnSignIn:CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        btnSignIn = findViewById(R.id.btnSignIn)

        supportActionBar?.hide()

        btnSignIn.setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }
}