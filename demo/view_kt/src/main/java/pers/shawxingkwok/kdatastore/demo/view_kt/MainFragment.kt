package pers.shawxingkwok.kdatastore.demo.view_kt

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import pers.shawxingkwok.androidutil.view.collectOnResume
import pers.shawxingkwok.kdatastore.demo.settings.Theme
import pers.shawxingkwok.kdatastore.demo.settings.Settings
import pers.shawxingkwok.kdatastore.demo.view_kt.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Settings.theme.collectOnResume {
            @SuppressLint("SetTextI18n")
            binding.btnTheme.text = "Theme: ${it.text}"
        }

        Settings.currentRole.collectOnResume{
            binding.btnUser.text =
                if (it == null)
                    "Set user"
                else
                    "Hello, ${it.name}"
        }

        onClickBtnTheme()
        onClickBtnUser()
    }

    private fun onClickBtnTheme(){
        binding.btnTheme.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(requireContext())

            val themes = Theme.values().map { it.text }.toTypedArray()
            val i = Settings.theme.value.ordinal

            dialogBuilder.setTitle("Set theme")

            dialogBuilder.setSingleChoiceItems(themes, i) { dialog, which ->
                Settings.theme.value = Theme.values()[which]
                dialog.dismiss()
            }

            dialogBuilder.show()
        }
    }

    private fun onClickBtnUser(){
        binding.btnUser.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setTitle("Set role")

            val allRoles = Settings.allRoles.value
            val currentRole = Settings.currentRole.value
            val items = allRoles.map { "${it.name} at age ${it.age}" }.toTypedArray() + "Disable"

            val i = currentRole?.let(allRoles::indexOf) ?: items.lastIndex

            dialogBuilder.setSingleChoiceItems(items, i) { dialog, which ->
                Settings.currentRole.value = allRoles.elementAtOrNull(which)
                dialog.dismiss()
            }

            dialogBuilder.show()
        }
    }
}