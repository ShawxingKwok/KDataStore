package pers.shawxingkwok.kdatastore.demo.view_kt

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import pers.shawxingkwok.androidutil.view.collectOnResume
import pers.shawxingkwok.kdatastore.demo.settings.Info
import pers.shawxingkwok.kdatastore.demo.settings.Theme
import pers.shawxingkwok.kdatastore.demo.settings.Settings
import pers.shawxingkwok.kdatastore.demo.view_kt.databinding.FragmentMainBinding
import pers.shawxingkwok.kdatastore.demo.view_kt.databinding.InfoBinding

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

        Settings.info.collectOnResume{ info ->
            binding.btnInfo.text =
                if (info == null)
                    "Set info"
                else{
                    val infix = if (info.isMale) "Mr" else "Miss"
                    "Hello, $infix ${info.lastName}"
                }
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
        binding.btnInfo.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setTitle("Info")

            val infoBinding = InfoBinding.inflate(layoutInflater, null, false)
            dialogBuilder.setView(infoBinding.root)

            val info = Settings.info.value
            if (info != null) {
                infoBinding.etFirstName.setText(info.firstName)
                infoBinding.etLastName.setText(info.lastName)
                infoBinding.male.isChecked = info.isMale
                infoBinding.female.isChecked = !info.isMale
            }

            dialogBuilder.setCancelable(false)

            dialogBuilder.setPositiveButton("Done"){ dialog, _ ->
                val firstName = infoBinding.etFirstName.text.toString()
                val lastName = infoBinding.etLastName.text.toString()
                val isMale = infoBinding.male.isChecked

                if (firstName.isEmpty()
                    || lastName.isEmpty()
                    || (!infoBinding.male.isChecked && !infoBinding.female.isChecked)
                )
                    Toast.makeText(requireContext(), "Not completed", Toast.LENGTH_SHORT).show()
                else {
                    Settings.info.value = Info(firstName, lastName, isMale)
                    dialog.dismiss()
                }
            }

            dialogBuilder.setNeutralButton("Clear"){ _, _ ->
                Settings.info.value = null
            }

            dialogBuilder.setNegativeButton("Cancel"){ dialog, _ ->
                dialog.cancel()
            }

            dialogBuilder.show()
        }
    }
}