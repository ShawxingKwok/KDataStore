package pers.apollokwok.prefstore.example.view.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import pers.apollokwok.prefstore.example.view.R
import pers.apollokwok.prefstore.example.view.collectOnResume
import pers.apollokwok.prefstore.example.view.data.ds.Language
import pers.apollokwok.prefstore.example.view.data.ds.Settings
import pers.apollokwok.prefstore.example.view.databinding.FragmentMainBinding

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
            Settings.language.toss{ language }
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