package pers.shawxingkwok.kdatastore.viewkt.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pers.shawxingkwok.kdatastore.viewkt.settings.Settings

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null)
            supportFragmentManager.commitNow {
                replace(R.id.container, MainFragment.newInstance())
            }

        // link stored data to the corresponding functionality.
        Settings.theme.onEach {
            AppCompatDelegate.setDefaultNightMode(it.mode)
            delegate.applyDayNight()
        }
        .launchIn(lifecycleScope)

        /* or use liveData
        Settings.theme.liveData.observe(this){
            AppCompatDelegate.setDefaultNightMode(it.mode)
            delegate.applyDayNight()
        }
        */
    }
}