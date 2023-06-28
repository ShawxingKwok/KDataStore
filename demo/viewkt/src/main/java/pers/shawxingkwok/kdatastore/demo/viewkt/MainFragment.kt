package pers.shawxingkwok.kdatastore.demo.viewkt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import pers.shawxingkwok.kdatastore.demo.settings.Settings
import pers.shawxingkwok.kdatastore.demo.viewkt.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    companion object {
        fun newInstance() = MainFragment()
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMainBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set the initial checked radioButton with `isDarkMode`
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
                    else -> error("")
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}