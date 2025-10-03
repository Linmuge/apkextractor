package info.muge.appshare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;

import info.muge.appshare.Constants;
import info.muge.appshare.R;

public class SPUtil {

    public static String getDisplayingExportPath(){
        return "内置存储/Download/AppKit/";
    }
    /**
     * 获取当前应用导出的内置主路径
     * @return 应用导出内置路径，最后没有文件分隔符，例如 /storage/emulated/0
     */
    public static String getInternalSavePath(){
        return Constants.PREFERENCE_SAVE_PATH_DEFAULT;
    }

    /**
     * 获取全局配置
     */
    public static SharedPreferences getGlobalSharedPreferences(@NonNull Context context){
        return context.getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE);
    }

    /**
     * 判断是否存储到了外置设备上
     * @return true-存储到了外置存储上
     */
    public static boolean getIsSaved2ExternalStorage(@NonNull Context context){
        return getGlobalSharedPreferences(context).getBoolean(Constants.PREFERENCE_STORAGE_PATH_EXTERNAL,Constants.PREFERENCE_STORAGE_PATH_EXTERNAL_DEFAULT);
    }

    /**
     * 获取外置存储的uri值
     */
    public static String getExternalStorageUri(@NonNull Context context){
        return Uri.parse(Constants.PREFERENCE_SAVE_PATH_DEFAULT).toString();
    }

    /**
     * 发送/接收 端口号，默认6565
     */
    public static int getPortNumber(@NonNull Context context){
        return getGlobalSharedPreferences(context).getInt(Constants.PREFERENCE_NET_PORT,Constants.PREFERENCE_NET_PORT_DEFAULT);
    }

    /**
     * 获取导出压缩包的扩展名
     */
    public static String getCompressingExtensionName(@NonNull Context context){
        return getGlobalSharedPreferences(context).getString(Constants.PREFERENCE_COMPRESSING_EXTENSION,Constants.PREFERENCE_COMPRESSING_EXTENSION_DEFAULT);
    }

    /**
     * 获取设备名称
     */
    public static String getDeviceName(@NonNull Context context){
        try{
            return getGlobalSharedPreferences(context)
                    .getString(Constants.PREFERENCE_DEVICE_NAME, Build.BRAND);
        }catch (Exception e){
            e.printStackTrace();
        }
        return Constants.PREFERENCE_DEVICE_NAME_DEFAULT;
    }
}
