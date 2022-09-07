package info.muge.appshare.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.util.DisplayMetrics;

import com.blankj.utilcode.util.BarUtils;

import info.muge.appshare.Constants;
import info.muge.appshare.utils.SPUtil;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_NAME="package_name";

    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setAndRefreshLanguage();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }


    public void setAndRefreshLanguage(){
        // 获得res资源对象
        Resources resources = getResources();
        // 获得屏幕参数：主要是分辨率，像素等。
        DisplayMetrics metrics = resources.getDisplayMetrics();
        // 获得配置对象
        Configuration config = resources.getConfiguration();
        //区别17版本（其实在17以上版本通过 config.locale设置也是有效的，不知道为什么还要区别）
        //在这里设置需要转换成的语言，也就是选择用哪个values目录下的strings.xml文件
        int value= SPUtil.getGlobalSharedPreferences(this).getInt(Constants.PREFERENCE_LANGUAGE,Constants.PREFERENCE_LANGUAGE_DEFAULT);
        Locale locale=null;
        switch (value){
            default:break;
            case Constants.LANGUAGE_FOLLOW_SYSTEM:locale=Locale.getDefault();break;
            case Constants.LANGUAGE_CHINESE:locale=Locale.SIMPLIFIED_CHINESE;break;
            case Constants.LANGUAGE_ENGLISH:locale=Locale.ENGLISH;break;
        }
        if(locale==null)return;
        config.setLocale(locale);
        resources.updateConfiguration(config, metrics);
    }
}
