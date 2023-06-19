package pers.shawxingkwok.kdatastore.viewjava.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import pers.shawxingkwok.kdatastore.viewjava.app.databinding.FragmentMainBinding;
import pers.shawxingkwok.kdatastore.viewjava.app.databinding.InfoBinding;
import pers.shawxingkwok.kdatastore.viewjava.settings.Info;
import pers.shawxingkwok.kdatastore.viewjava.settings.Settings;
import pers.shawxingkwok.kdatastore.viewjava.settings.Theme;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentMainBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Settings.getTheme().getLiveData().observe(getViewLifecycleOwner(), (theme)->{
            String text = "Theme: " + theme.getText();
            binding.btnTheme.setText(text);
        });

        Settings.getInfo().getLiveData().observe(getViewLifecycleOwner(), (info)->{
            String text;

            if (info == null)
                text = "Set info";
            else if(info.isMale())
                text = "Hello, Mr " + info.getLastName();
            else
                text = "Hello, Miss " + info.getLastName();

            binding.btnInfo.setText(text);
        });

        onClickBtnTheme();
        onClickBtnUser();
    }

    private void onClickBtnTheme(){
        binding.btnTheme.setOnClickListener((view)-> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());

            String[] items = new String[Theme.values().length];
            for (int i = 0; i < Theme.values().length; i++) {
                items[i] = Theme.values()[i].getText();
            }

            int i = Settings.getTheme().getValue().ordinal();

            dialogBuilder.setTitle("Set theme");

            dialogBuilder.setSingleChoiceItems(items, i, (dialog, which) -> {
                Theme newTheme = Theme.values()[which];
                Settings.getTheme().setValue(newTheme);
                dialog.dismiss();
            });

            dialogBuilder.show();
        });
    }

    private void onClickBtnUser(){
        binding.btnInfo.setOnClickListener((view)-> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            dialogBuilder.setTitle("Info");

            InfoBinding infoBinding = InfoBinding.inflate(getLayoutInflater(), null, false);
            dialogBuilder.setView(infoBinding.getRoot());

            Info info = Settings.getInfo().getValue();
            if (info != null) {
                infoBinding.etFirstName.setText(info.getFirstName());
                infoBinding.etLastName.setText(info.getLastName());
                infoBinding.male.setChecked(info.isMale());
                infoBinding.female.setChecked(!info.isMale());
            }

            dialogBuilder.setPositiveButton("Done", (dialog, which) ->{
                String firstName = infoBinding.etFirstName.getText().toString();
                String lastName = infoBinding.etLastName.getText().toString();
                boolean isMale = infoBinding.male.isChecked();

                if (firstName.isEmpty()
                    || lastName.isEmpty()
                    || (!infoBinding.male.isChecked() && !infoBinding.female.isChecked())
                )
                    Toast.makeText(requireContext(), "Not completed", Toast.LENGTH_SHORT).show();
                else {
                    Info newInfo = new Info(firstName, lastName, isMale);
                    Settings.getInfo().setValue(newInfo);
                }

                dialog.dismiss();
            });

            dialogBuilder.setNeutralButton("Clear", (dialog, which) -> {
                Settings.getInfo().setValue(null);
                dialog.dismiss();
            });

            dialogBuilder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });

            dialogBuilder.show();
        });
    }
}