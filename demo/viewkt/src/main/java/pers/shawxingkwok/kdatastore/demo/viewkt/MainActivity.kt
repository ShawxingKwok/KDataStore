package pers.shawxingkwok.kdatastore.demo.viewkt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pers.shawxingkwok.kdatastore.demo.settings.Settings

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null)
            supportFragmentManager.commitNow {
                replace(R.id.container, MainFragment.newInstance())
            }

        // link `isDarkMode` with `theme`.
        Settings.isDarkMode.onEach {
            val mode = when(it){
                null -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                true -> AppCompatDelegate.MODE_NIGHT_YES
                false -> AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            delegate.applyDayNight()
        }
        .launchIn(lifecycleScope)

        /* or use liveData
        Settings.isDarkMode.liveData.observe(this){
            val mode = when(it){
                null -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                true -> AppCompatDelegate.MODE_NIGHT_YES
                false -> AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            delegate.applyDayNight()
        }
        */
    }
}