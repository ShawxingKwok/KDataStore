package pers.apollokwok.kdatastore.examples.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pers.apollokwok.kdatastore.examples.view.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        observeDarkMode()
        binding.btnChooseDarkMode.setOnClickListener { updateDarkMode() }
    }

    private fun observeDarkMode(){
        Settings.darkMode
            .onEach { mode ->
                AppCompatDelegate.setDefaultNightMode(mode.corresponding)
                delegate.applyDayNight()
                binding.btnChooseDarkMode.text = mode.text
            }
            .launchIn(lifecycleScope)
    }

    private fun updateDarkMode() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("choose dark mode")

        val modes = DarkMode.values().map { it.text }.toTypedArray()
        val checkedItem = modes.indexOf(binding.btnChooseDarkMode.text)
        builder.setSingleChoiceItems(modes, checkedItem) { dialog, index ->
            val selectedMode = DarkMode.values()[index]
            Settings.darkMode.toss(selectedMode)
            dialog.dismiss()
        }

        builder.create().show()
    }
}