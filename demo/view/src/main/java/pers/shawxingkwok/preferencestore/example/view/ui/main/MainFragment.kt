package pers.shawxingkwok.preferencestore.example.view.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.ServiceConnection
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.edit
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import pers.shawxingkwok.android.KLog
import pers.shawxingkwok.android.view.collectOnResume
import pers.shawxingkwok.preferencestore.example.view.App
import pers.shawxingkwok.preferencestore.example.view.MLog
import pers.shawxingkwok.preferencestore.example.view.R
import pers.shawxingkwok.preferencestore.example.view.data.ds.Language
import pers.shawxingkwok.preferencestore.example.view.data.ds.Settings
import pers.shawxingkwok.preferencestore.example.view.databinding.FragmentMainBinding

class MainFragment : Fragment(R.layout.fragment_main) {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
        binding.btnLanguage.setOnClickListener { onClickBtnLanguage() }
        onSetLocation()
    }

    @SuppressLint("SetTextI18n")
    private fun observe(){
        Settings.language.collectOnResume{
            binding.btnLanguage.text = "Language: $it"
        }

        Settings.location.collectOnResume {
            binding.editTextLatitude.setText(it.lat.toString())
            binding.editTextLongitude.setText(it.lng.toString())
        }
    }

    private fun onClickBtnLanguage(){
        val popupMenu = PopupMenu(requireContext(), binding.btnLanguage)

        popupMenu.inflate(R.menu.menu_lanugages)

        Language.values()
            .forEach {
                popupMenu.menu.add("$it")
            }

        popupMenu.setOnMenuItemClickListener { item ->
            val language = Language.valueOf(value = item.title!!.toString())
            Settings.language.toss(language)
            true
        }

        popupMenu.show()
    }

    private fun onSetLocation(){
        binding.editTextLatitude.doAfterTextChanged { editable ->
            editable ?: return@doAfterTextChanged

            Settings.location.toss{
                it.copy(lat = editable.toString().toDouble())
            }
        }

        binding.editTextLongitude.doAfterTextChanged { editable ->
            editable ?: return@doAfterTextChanged

            Settings.location.toss{
                it.copy(lng = editable.toString().toDouble())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}