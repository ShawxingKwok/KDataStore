package pers.shawxingkwok.benchmark

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // here tests benchmark since androidTest doesn't work on my physical phone.
        Benchmark(this)
    }
}