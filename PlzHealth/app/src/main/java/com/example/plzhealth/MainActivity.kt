package com.example.plzhealth

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        replaceFragment(HealthPointFragment())

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //하단바
        val btnHealth = findViewById<TextView>(R.id.btnHealth)
        val btnToday = findViewById<TextView>(R.id.btnToday)
        val btnSearch = findViewById<TextView>(R.id.btnSearch)
        val btnMypage = findViewById<TextView>(R.id.btnMypage)

        btnHealth.setOnClickListener {
            replaceFragment(HealthPointFragment())
        }

        btnToday.setOnClickListener {
            replaceFragment(TodayFragment())
        }

        btnSearch.setOnClickListener {
            replaceFragment(SearchFragment())
        }

        btnMypage.setOnClickListener {
            replaceFragment(MypageFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}