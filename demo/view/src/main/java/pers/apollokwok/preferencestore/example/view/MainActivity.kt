package pers.apollokwok.preferencestore.example.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pers.apollokwok.preferencestore.example.view.ui.main.MainFragment
import java.io.File

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