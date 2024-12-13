package info.muge.appshare

import android.app.Application
import info.muge.appshare.utils.SPUtil
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        val settings = SPUtil.getGlobalSharedPreferences(this)
        val night_mode = settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT)
        AppCompatDelegate.setDefaultNightMode(night_mode)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }


    companion object {

        lateinit var instance: Application
            private set
    }
}