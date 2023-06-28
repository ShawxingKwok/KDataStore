package pers.shawxingkwok.kdatastore.demo.viewjava;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import pers.shawxingkwok.kdatastore.demo.settings.Settings;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow();
        }

        // link `isDarkMode` with `theme` via `liveData`
        Settings.isDarkMode().getLiveData().observe(this, (isDark)->{
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
    }
}