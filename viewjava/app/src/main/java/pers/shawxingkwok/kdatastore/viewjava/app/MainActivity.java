package pers.shawxingkwok.kdatastore.viewjava.app;

import android.annotation.SuppressLint;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import pers.shawxingkwok.kdatastore.KDataStore;
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

        // link stored data to the corresponding functionality via LiveData.
        Settings.getTheme().getLiveData().observe(this, (theme)->{
            switch (theme){
                case FOLLOW_SYSTEM:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;

                case DARK:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;

                case LIGHT:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
            }

            getDelegate().applyDayNight();
        });
    }
}