package pers.shawxingkwok.kdatastore.viewjava.settings;

import androidx.appcompat.app.AppCompatDelegate;

public enum Theme {
    FOLLOW_SYSTEM, DARK, LIGHT;

    public int getMode(){
        switch (this){
            case FOLLOW_SYSTEM: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case DARK: return AppCompatDelegate.MODE_NIGHT_YES;
            case LIGHT: return AppCompatDelegate.MODE_NIGHT_NO;
            default: throw new IllegalStateException("");
        }
    }
}