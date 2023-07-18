package pers.shawxingkwok.kdatastore.demo.settings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import pers.shawxingkwok.kdatastore.KDataStore;

@RunWith(AndroidJUnit4.class)
public class ATest {
    @Test
    public void start(){
        if (Settings.INSTANCE.exist()) {
            // ...
            Settings.INSTANCE.delete();
        }
    }
}