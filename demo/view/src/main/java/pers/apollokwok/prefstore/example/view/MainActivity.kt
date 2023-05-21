package pers.apollokwok.prefstore.example.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pers.apollokwok.prefstore.example.view.ui.main.MainFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, MainFragment())
                .commitNow()
        }
    }
}