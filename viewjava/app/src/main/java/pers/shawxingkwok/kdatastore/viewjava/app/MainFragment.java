package pers.shawxingkwok.kdatastore.viewjava.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import pers.shawxingkwok.kdatastore.viewjava.app.databinding.FragmentMainBinding;
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

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set display with stored data
        // LiveData observation is needless here in this case.
        switch (Settings.getTheme().getValue()){
            case FOLLOW_SYSTEM:
                binding.rbFollowSystem.setChecked(true);
                break;

            case DARK:
                binding.rbDark.setChecked(true);
                break;

            case LIGHT:
                binding.rbLight.setChecked(true);
                break;
        }

        // update stored data in Listener
        binding.rgTheme.setOnCheckedChangeListener(
            (group, checkedId) -> {
                Theme newTheme;

                switch (checkedId){
                    case R.id.rb_followSystem:
                        newTheme = Theme.FOLLOW_SYSTEM;
                        break;

                    case R.id.rb_dark:
                        newTheme = Theme.DARK;
                        break;

                    case R.id.rb_light:
                        newTheme = Theme.LIGHT;
                        break;

                    default:
                        throw new IllegalStateException("Unexpected value: " + checkedId);
                }

                Settings.getTheme().setValue(newTheme);
            }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}