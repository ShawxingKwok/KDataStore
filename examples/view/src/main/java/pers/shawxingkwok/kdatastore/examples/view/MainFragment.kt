package pers.shawxingkwok.kdatastore.examples.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainFragment : Fragment(R.layout.fragment_main) {
    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switch = view.findViewById<SwitchCompat>(R.id.switch_setDarkMode)

        Settings.isDarkMode
            .onEach { switch.isChecked = it }
            .launchIn(lifecycleScope)

        switch.setOnCheckedChangeListener { _, isChecked ->
            Settings.isDarkMode.toss(isChecked)
        }
    }
}