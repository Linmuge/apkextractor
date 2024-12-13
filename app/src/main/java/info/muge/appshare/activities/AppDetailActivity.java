package info.muge.appshare.activities;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import info.muge.appshare.Constants;
import info.muge.appshare.Global;
import info.muge.appshare.R;
import info.muge.appshare.items.AppItem;
import info.muge.appshare.tasks.GetPackageInfoViewTask;
import info.muge.appshare.tasks.GetSignatureInfoTask;
import info.muge.appshare.tasks.HashTask;
import info.muge.appshare.ui.AssemblyView;
import info.muge.appshare.ui.SignatureView;
import info.muge.appshare.ui.ToastManager;
import info.muge.appshare.utils.OutputUtil;
import info.muge.appshare.utils.SPUtil;
import info.muge.appshare.utils.ViewExtsKt;

public class AppDetailActivity extends BaseActivity implements View.OnClickListener{
    private AppItem appItem;
    private final BroadcastReceiver uninstall_receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)||intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)){
                    String data=intent.getDataString();
                    String package_name=data.substring(data.indexOf(":")+1);
                    if(package_name.equalsIgnoreCase(appItem.getPackageName()))finish();
                }
            }catch (Exception e){e.printStackTrace();}
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //appItem=getIntent().getParcelableExtra(EXTRA_PARCELED_APP_ITEM);
        try{
            synchronized (Global.app_list) {
                appItem=Global.getAppItemByPackageNameFromList(Global.app_list,getIntent().getStringExtra(EXTRA_PACKAGE_NAME));
            }
        }catch (Exception e){e.printStackTrace();}
        if(appItem==null){
            ToastManager.showToast(this,"(-_-)The AppItem info is null, try to restart this application.",Toast.LENGTH_SHORT);
            finish();
            return;
        }
        setContentView(R.layout.activity_app_detail);

        Toolbar toolbar = findViewById(R.id.toolbar_app_detail);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(appItem.getAppName());


        PackageInfo packageInfo=appItem.getPackageInfo();

        ((TextView)findViewById(R.id.app_detail_name)).setText(appItem.getAppName());
        ((TextView)findViewById(R.id.app_detail_version_name_title)).setText(appItem.getVersionName());
        ((ImageView)findViewById(R.id.app_detail_icon)).setImageDrawable(appItem.getIcon());

        ((TextView)findViewById(R.id.app_detail_package_name)).setText(appItem.getPackageName());
        ((TextView)findViewById(R.id.app_detail_version_name)).setText(appItem.getVersionName());
        ((TextView)findViewById(R.id.app_detail_version_code)).setText(String.valueOf(appItem.getVersionCode()));
        ((TextView)findViewById(R.id.app_detail_size)).setText(Formatter.formatFileSize(this,appItem.getSize()));
        ((TextView)findViewById(R.id.app_detail_install_time)).setText(SimpleDateFormat.getDateTimeInstance().format(new Date(packageInfo.firstInstallTime)));
        ((TextView)findViewById(R.id.app_detail_update_time)).setText(SimpleDateFormat.getDateTimeInstance().format(new Date(packageInfo.lastUpdateTime)));
        ((TextView)findViewById(R.id.app_detail_minimum_api)).setText(String.valueOf(packageInfo.applicationInfo.minSdkVersion));
        ((TextView)findViewById(R.id.app_detail_target_api)).setText(String.valueOf(packageInfo.applicationInfo.targetSdkVersion));
        ((TextView)findViewById(R.id.app_detail_is_system_app)).setText(getResources().getString((appItem.getPackageInfo().applicationInfo.flags& ApplicationInfo.FLAG_SYSTEM)>0?R.string.word_yes:R.string.word_no));
        ((TextView)findViewById(R.id.app_detail_path_value)).setText(appItem.getPackageInfo().applicationInfo.sourceDir);
        ((TextView)findViewById(R.id.app_detail_installer_name_value)).setText(appItem.getInstallSource());
        ((TextView)findViewById(R.id.app_detail_uid)).setText(String.valueOf(appItem.getPackageInfo().applicationInfo.uid));
        ((TextView)findViewById(R.id.app_detail_launcher_value)).setText(appItem.getLaunchingClass());


        new GetPackageInfoViewTask(this, appItem.getPackageInfo(), appItem.getStaticReceiversBundle(), (AssemblyView) findViewById(R.id.app_detail_assembly), new GetPackageInfoViewTask.CompletedCallback() {
            @Override
            public void onViewsCreated() {
                findViewById(R.id.app_detail_card_pg).setVisibility(View.GONE);
            }
        }).start();

        if(SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE,Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)){
            findViewById(R.id.app_detail_signature_att).setVisibility(View.VISIBLE);
            findViewById(R.id.app_detail_sign_pg).setVisibility(View.VISIBLE);
            new GetSignatureInfoTask(this, appItem.getPackageInfo(), (SignatureView) findViewById(R.id.app_detail_signature), new GetSignatureInfoTask.CompletedCallback() {
                @Override
                public void onCompleted() {
                    findViewById(R.id.app_detail_sign_pg).setVisibility(View.GONE);
                }
            }).start();
        }

        if(SPUtil.getGlobalSharedPreferences(this).getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH,Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT)){
            findViewById(R.id.app_detail_hash_att).setVisibility(View.VISIBLE);
            findViewById(R.id.app_detail_hash).setVisibility(View.VISIBLE);
            new HashTask(appItem.getFileItem(), HashTask.HashType.MD5, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_md5_pg).setVisibility(View.GONE);
                    TextView tv_md5=findViewById(R.id.detail_hash_md5_value);
                    tv_md5.setVisibility(View.VISIBLE);
                    tv_md5.setText(result);
                }
            }).start();
            new HashTask(appItem.getFileItem(), HashTask.HashType.SHA1, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_sha1_pg).setVisibility(View.GONE);
                    TextView tv_sha1=findViewById(R.id.detail_hash_sha1_value);
                    tv_sha1.setVisibility(View.VISIBLE);
                    tv_sha1.setText(result);
                }
            }).start();
            new HashTask(appItem.getFileItem(), HashTask.HashType.SHA256, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_sha256_pg).setVisibility(View.GONE);
                    TextView tv_sha256=findViewById(R.id.detail_hash_sha256_value);
                    tv_sha256.setVisibility(View.VISIBLE);
                    tv_sha256.setText(result);
                }
            }).start();
            new HashTask(appItem.getFileItem(), HashTask.HashType.CRC32, new HashTask.CompletedCallback() {
                @Override
                public void onHashCompleted(@NonNull String result) {
                    findViewById(R.id.detail_hash_crc32_pg).setVisibility(View.GONE);
                    TextView tv_crc32=findViewById(R.id.detail_hash_crc32_value);
                    tv_crc32.setVisibility(View.VISIBLE);
                    tv_crc32.setText(result);
                }
            }).start();
        }

        try{
            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            intentFilter.addDataScheme("package");
            registerReceiver(uninstall_receiver,intentFilter);
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void onClick(View v){
        var id = v.getId();
        if (id == R.id.app_detail_run_area){
            try{
                startActivity(getPackageManager().getLaunchIntentForPackage(appItem.getPackageName()));
            }catch (Exception e){
                ToastManager.showToast(AppDetailActivity.this,"应用没有界面,无法运行",Toast.LENGTH_SHORT);
            }
        }else  if (id == R.id.app_detail_export_area){
            final List<AppItem> single_list=getSingleItemArrayList();
            final AppItem item=single_list.get(0);
            Global.checkAndExportCertainAppItemsToSetPathWithoutShare(this,single_list , false,new Global.ExportTaskFinishedListener() {
                @Override
                public void onFinished(@NonNull String error_message) {
                    if(!error_message.trim().equals("")){
                        new AlertDialog.Builder(AppDetailActivity.this)
                                .setTitle(getResources().getString(R.string.exception_title))
                                .setMessage(getResources().getString(R.string.exception_message)+error_message)
                                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), (dialog, which) -> {})
                                .show();
                        return;
                    }
                    ToastManager.showToast(AppDetailActivity.this,getResources().getString(R.string.toast_export_complete)+" "
                            + SPUtil.getDisplayingExportPath()
                            + OutputUtil.getWriteFileNameForAppItem(AppDetailActivity.this,single_list.get(0),(item.exportData||item.exportObb)?
                            SPUtil.getCompressingExtensionName(AppDetailActivity.this):"apk",1),Toast.LENGTH_SHORT);
                }
            });
        }else  if (id == R.id.app_detail_share_area){
            Global.shareCertainAppsByItems(this,getSingleItemArrayList());
        }else  if (id == R.id.app_detail_detail_area){
            Intent intent=new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", appItem.getPackageName(), null));
            startActivity(intent);
        }else  if (id == R.id.app_detail_market_area){
            try{
                Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appItem.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }catch (Exception e){
                ToastManager.showToast(AppDetailActivity.this,e.toString(),Toast.LENGTH_SHORT);
            }
        }else  if (id == R.id.app_detail_package_name_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_package_name)).getText().toString());
        }else  if (id == R.id.app_detail_version_name_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_version_name)).getText().toString());
        }else  if (id == R.id.app_detail_version_code_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_version_code)).getText().toString());
        }else  if (id == R.id.app_detail_size_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_size)).getText().toString());
        }else  if (id == R.id.app_detail_install_time_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_install_time)).getText().toString());
        }else  if (id == R.id.app_detail_update_time_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_update_time)).getText().toString());
        }else  if (id == R.id.app_detail_minimum_api_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_minimum_api)).getText().toString());
        }else  if (id == R.id.app_detail_target_api_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_target_api)).getText().toString());
        }else  if (id == R.id.app_detail_is_system_app_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_is_system_app)).getText().toString());
        }else  if (id == R.id.detail_hash_md5){
            final String value=((TextView)findViewById(R.id.detail_hash_md5_value)).getText().toString();
            if(!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value);
        }else  if (id == R.id.detail_hash_sha1){
            final String value=((TextView)findViewById(R.id.detail_hash_sha1_value)).getText().toString();
            if(!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value);
        }else  if (id == R.id.detail_hash_sha256){
            final String value=((TextView)findViewById(R.id.detail_hash_sha256_value)).getText().toString();
            if(!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value);
        }else  if (id == R.id.detail_hash_crc32){
            final String value=((TextView)findViewById(R.id.detail_hash_crc32_value)).getText().toString();
            if(!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value);
        }else  if (id == R.id.app_detail_path_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_path_value)).getText().toString());
        }else  if (id == R.id.app_detail_installer_name_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_installer_name_value)).getText().toString());

        }else  if (id == R.id.app_detail_uid_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_uid)).getText().toString());

        }else  if (id == R.id.app_detail_launcher_area){
            clip2ClipboardAndShowSnackbar(((TextView)findViewById(R.id.app_detail_launcher_value)).getText().toString());

        }else{
            ViewExtsKt.toast("功能未开放");
        }
    }

    private void clip2ClipboardAndShowSnackbar(String s){
        try{
            ClipboardManager manager=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("message",s));
            Snackbar.make(findViewById(android.R.id.content),getResources().getString(R.string.snack_bar_clipboard),Snackbar.LENGTH_SHORT).show();
        }catch (Exception e){e.printStackTrace();}
    }

    /**
     * 构造包含单个副本AppItem的ArrayList
     */
    private @NonNull ArrayList<AppItem>getSingleItemArrayList(){
        ArrayList<AppItem>list=new ArrayList<>();
        AppItem item=new AppItem(appItem,false,false);

        list.add(item);
        return list;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            checkHeightAndFinish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkHeightAndFinish(){
        if(Build.VERSION.SDK_INT>=28){ //根布局项目太多时低版本Android会引发一个底层崩溃。版本号暂定28
            ActivityCompat.finishAfterTransition(this);
        }else {
            if(((AssemblyView)findViewById(R.id.app_detail_assembly)).getIsExpanded()){
                finish();
            }else{
                ActivityCompat.finishAfterTransition(this);
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        try{
            unregisterReceiver(uninstall_receiver);
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case android.R.id.home:{
                checkHeightAndFinish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
