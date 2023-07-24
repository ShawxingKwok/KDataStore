package pers.shawxingkwok.kdatastore.demo.viewjava;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import pers.shawxingkwok.kdatastore.demo.settings.Settings;
import pers.shawxingkwok.kdatastore.demo.viewjava.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("FieldCanBeLocal")
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // link `isDarkMode` with `theme` via `liveData`
        Settings.isDarkMode().getLiveData().observe(this, (isDark) -> {
            int mode;
            if (isDark == null)
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            else if (isDark)
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            else
                mode = (AppCompatDelegate.MODE_NIGHT_NO);

            AppCompatDelegate.setDefaultNightMode(mode);
            getDelegate().applyDayNight();
        });

        // set the initial checked radioButton with `isDarkMode`
        if (Settings.isDarkMode().getValue() == null)
            binding.rbFollowSystem.setChecked(true);
        else if (Settings.isDarkMode().getValue())
            binding.rbDark.setChecked(true);
        else
            binding.rbLight.setChecked(true);

        // update `isDarkMode` in Listener
        binding.rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_followSystem) {
                Settings.isDarkMode().setValue(null);
            } else if (checkedId == R.id.rb_dark) {
                Settings.isDarkMode().setValue(true);
            } else if (checkedId == R.id.rb_light) {
                Settings.isDarkMode().setValue(false);
            } else {
                throw new IllegalStateException("Unexpected value: " + checkedId);
            }
        });
    }
}