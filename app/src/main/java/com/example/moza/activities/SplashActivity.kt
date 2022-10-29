package com.example.moza.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.example.moza.R
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        Handler().postDelayed({
            if(user == null) {
                startActivity(Intent(this , MainActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this , HomeActivity::class.java))
                finish()
            }
        },2000)


    }
}