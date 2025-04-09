package info.muge.appshare.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.PermissionChecker;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import info.muge.appshare.Constants;
import info.muge.appshare.R;
import info.muge.appshare.adapters.MyPagerAdapter;
import info.muge.appshare.fragments.AppFragment;
import info.muge.appshare.fragments.OperationCallback;
import info.muge.appshare.ui.AppItemSortConfigDialog;
import info.muge.appshare.ui.SortConfigDialogCallback;
import info.muge.appshare.utils.EnvironmentUtil;
import info.muge.appshare.utils.SPUtil;
import info.muge.appshare.utils.ViewExtsKt;

public class MainActivity extends BaseActivity implements View.OnClickListener,ViewPager.OnPageChangeListener, CompoundButton.OnCheckedChangeListener
 , OperationCallback {

    private final AppFragment appFragment=new AppFragment();

    private int currentSelection=0;
    private boolean isSearchMode=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);

        /*ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            ViewExtsKt.setHeight(toolbar, insets.getSystemWindowInsetTop()+toolbar.getMeasuredHeight());
            v.setPaddingRelative(0, insets.getSystemWindowInsetTop(),0,0);
            return insets.consumeSystemWindowInsets();
        });*/
        setSupportActionBar(toolbar);

        try{
            getSupportActionBar()
                    .setTitle(getResources().getString(R.string.app_name));
        }catch (Exception e){e.printStackTrace();}

        TabLayout tabLayout = findViewById(R.id.main_tablayout);
        ViewPager viewPager = findViewById(R.id.main_viewpager);

        appFragment.setOperationCallback(this);

        SearchView searchView = findViewById(R.id.searchview);
        searchView.setOnSearchClickListener(view -> {
            openSearchMode();
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (isSearchMode){
                    if (newText.isBlank()){
                        appFragment.setSearchMode(false);
                    }else {
                        appFragment.updateSearchModeKeywords(newText.toString());
                    }
                }
                return true;
            }
        });
        searchView.setOnCloseListener(() -> {
            closeSearchMode();
            return false;
        });
        viewPager.setAdapter(new MyPagerAdapter(this,getSupportFragmentManager(),appFragment));
        tabLayout.setupWithViewPager(viewPager,true);
        viewPager.addOnPageChangeListener(this);

    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {}

    @Override
    public void onPageSelected(int i) {
        this.currentSelection=i;
    }

    @Override
    public void onPageScrollStateChanged(int i) {}

    @Override
    public void onClick(View v){}

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {}

    @Override
    public void onItemLongClickedAndMultiSelectModeOpened(@NonNull Fragment fragment) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==0){
            if(grantResults.length==0)return;
            if(grantResults[0]==PermissionChecker.PERMISSION_GRANTED){
                sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id==R.id.action_view){
            if(isSearchMode)return false;
            final SharedPreferences settings= SPUtil.getGlobalSharedPreferences(this);
            final SharedPreferences.Editor editor=settings.edit();
            if(currentSelection==0){
                final int mode_app=settings.getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT);
                final int result_app=mode_app==0?1:0;
                editor.putInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,result_app);
                editor.apply();
                appFragment.setViewMode(result_app);
            }
        }

        if(id==R.id.action_sort){
            if(currentSelection==0){
                AppItemSortConfigDialog appItemSortConfigDialog=new AppItemSortConfigDialog(this, new SortConfigDialogCallback() {
                    @Override
                    public void onOptionSelected(int value) {
                        appFragment.sortGlobalListAndRefresh(value);
                    }
                });
                appItemSortConfigDialog.show();
            }
        }
        if(id==R.id.action_settings){
            startActivityForResult(new Intent(this,SettingActivity.class),REQUEST_CODE_SETTINGS);

        }
        if(id==R.id.action_about){

            View dialogView=LayoutInflater.from(this).inflate(R.layout.dialog_about, null);

            new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                    .setTitle(EnvironmentUtil.getAppName(this)+"("+EnvironmentUtil.getAppVersionName(this)+")")
                    .setIcon(R.mipmap.ic_launcher_round)
                    .setCancelable(true)
                    .setView(dialogView)
                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), (arg0, arg1) -> {}).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private static final int REQUEST_CODE_SETTINGS=0;
    private static final int REQUEST_CODE_RECEIVING_FILES=1;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            default:
                break;
            case REQUEST_CODE_SETTINGS: {
                if (resultCode == RESULT_OK) {
                    finish();
                    startActivity(new Intent(this, MainActivity.class));
                }
            }
            break;
            case REQUEST_CODE_RECEIVING_FILES: {
                if (resultCode == RESULT_OK) {
                    sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                }
            }
            break;
        }
    }

    private void openSearchMode(){
        isSearchMode=true;
        appFragment.setSearchMode(true);
    }

    private void closeSearchMode(){
        isSearchMode=false;
        appFragment.setSearchMode(false);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            checkAndExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private void checkAndExit(){
        switch(currentSelection){
            default:break;
            case 0:{
                if(appFragment.isMultiSelectMode()){
                    appFragment.closeMultiSelectMode();
                    return;
                }
            }
            break;
        }
        if(isSearchMode){
            closeSearchMode();
            return;
        }
        finish();
    }
}
