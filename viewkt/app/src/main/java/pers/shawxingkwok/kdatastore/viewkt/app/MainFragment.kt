package pers.shawxingkwok.kdatastore.viewkt.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import pers.shawxingkwok.androidutil.view.collectOnResume
import pers.shawxingkwok.kdatastore.viewkt.settings.Theme
import pers.shawxingkwok.kdatastore.viewkt.settings.Settings
import pers.shawxingkwok.kdatastore.viewkt.app.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    companion object {
        fun newInstance() = MainFragment()
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Collection is needless here in this case.
        when (Settings.theme.value) {
            Theme.FOLLOW_SYSTEM -> binding.rbFollowSystem
            Theme.DARK -> binding.rbDark
            Theme.LIGHT -> binding.rbLight
        }
        .isChecked = true

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId -> setTheme(checkedId) }
    }

    private fun setTheme(@IdRes id: Int){
        Settings.theme.value =
            when(id){
                R.id.rb_followSystem -> Theme.FOLLOW_SYSTEM
                R.id.rb_dark -> Theme.DARK
                R.id.rb_light -> Theme.LIGHT
                else -> error("")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}