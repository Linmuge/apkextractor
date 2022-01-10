package info.muge.appshare

import android.app.Application
import info.muge.appshare.utils.SPUtil
import androidx.appcompat.app.AppCompatDelegate

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        val settings = SPUtil.getGlobalSharedPreferences(this)
        val night_mode = settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT)
        AppCompatDelegate.setDefaultNightMode(night_mode)
    }


    companion object {

        lateinit var instance: Application
            private set
    }
}