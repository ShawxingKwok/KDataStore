package pers.shawxingkwok.kdatastore.demo.viewkt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pers.shawxingkwok.kdatastore.demo.viewkt.databinding.ActivityMainBinding
import pers.shawxingkwok.settings.Settings

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // link `isDarkMode` to `theme` via flow
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

        /* or via liveData
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

        // set the initial checked radioButton
        when (Settings.isDarkMode.value) {
            null -> binding.rbFollowSystem
            true -> binding.rbDark
            false -> binding.rbLight
        }
        .isChecked = true

        // update `isDarkMode` in Listener
        binding.rgTheme.setOnCheckedChangeListener { _, id ->
            Settings.isDarkMode.value =
                when(id){
                    R.id.rb_followSystem -> null
                    R.id.rb_dark -> true
                    R.id.rb_light -> false
                    else -> throw Error()
                }
        }
    }
}