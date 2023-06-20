package pers.shawxingkwok.kdatastore.viewjava.app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import pers.shawxingkwok.kdatastore.viewjava.settings.Settings;

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

        Settings.getTheme().getLiveData().observe(this, (theme)->{
            AppCompatDelegate.setDefaultNightMode(theme.getMode());
            getDelegate().applyDayNight();
        });
    }
}