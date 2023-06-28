package pers.shawxingkwok.kdatastore.demo.viewjava;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import pers.shawxingkwok.kdatastore.demo.settings.Settings;
import pers.shawxingkwok.kdatastore.demo.viewjava.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {
    public static MainFragment newInstance() {
        return new MainFragment();
    }
    private FragmentMainBinding binding;

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

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set the initial checked radioButton with `isDarkMode`
        if (Settings.isDarkMode().getValue() == null)
            binding.rbFollowSystem.setChecked(true);
        else if (Settings.isDarkMode().getValue())
            binding.rbDark.setChecked(true);
        else
            binding.rbLight.setChecked(true);

        // update `isDarkMode` in Listener
        binding.rgTheme.setOnCheckedChangeListener(
            (group, checkedId) -> {
                switch (checkedId){
                    case R.id.rb_followSystem:
                        Settings.isDarkMode().setValue(null);
                        break;

                    case R.id.rb_dark:
                        Settings.isDarkMode().setValue(true);
                        break;

                    case R.id.rb_light:
                        Settings.isDarkMode().setValue(false);
                        break;

                    default:
                        throw new IllegalStateException("Unexpected value: " + checkedId);
                }
            }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}