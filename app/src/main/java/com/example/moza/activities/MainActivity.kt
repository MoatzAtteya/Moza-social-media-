package com.example.moza.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.moza.R
import com.example.moza.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //supportFragmentManager.beginTransaction().replace(R.id.mainContainer , WelcomeFragment()).commit()

        val navHostFragment =
            findNavController(R.id.nav_host_fragment_activity_parent_dashboard)



    }



}