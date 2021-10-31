package info.muge.apkextractor;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import info.muge.apkextractor.utils.SPUtil;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences settings= SPUtil.getGlobalSharedPreferences(this);
        int night_mode=settings.getInt(Constants.PREFERENCE_NIGHT_MODE,Constants.PREFERENCE_NIGHT_MODE_DEFAULT);
        AppCompatDelegate.setDefaultNightMode(night_mode);
    }
}
