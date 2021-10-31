package info.muge.apkextractor.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.core.content.PermissionChecker;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;

import info.muge.apkextractor.Constants;
import info.muge.apkextractor.Global;
import info.muge.apkextractor.R;
import info.muge.apkextractor.adapters.MyPagerAdapter;
import info.muge.apkextractor.fragments.AppFragment;
import info.muge.apkextractor.fragments.ImportFragment;
import info.muge.apkextractor.fragments.OperationCallback;
import info.muge.apkextractor.ui.AppItemSortConfigDialog;
import info.muge.apkextractor.ui.ImportItemSortConfigDialog;
import info.muge.apkextractor.ui.SortConfigDialogCallback;
import info.muge.apkextractor.utils.EnvironmentUtil;
import info.muge.apkextractor.utils.SPUtil;

public class MainActivity extends BaseActivity implements View.OnClickListener,ViewPager.OnPageChangeListener, CompoundButton.OnCheckedChangeListener
, NavigationView.OnNavigationItemSelectedListener , OperationCallback {

    private final AppFragment appFragment=new AppFragment();
    private final ImportFragment importFragment=new ImportFragment();

    private int currentSelection=0;
    private boolean isSearchMode=false;
    private EditText edit_search;
    private Menu menu;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try{
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        }catch (Exception e){e.printStackTrace();}

        drawerLayout=findViewById(R.id.main_drawer);
        ActionBarDrawerToggle actionBarDrawerToggle=new ActionBarDrawerToggle(this,drawerLayout,toolbar,0,0){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                EnvironmentUtil.hideInputMethod(MainActivity.this);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        NavigationView navigationView=findViewById(R.id.main_navigation);
        navigationView.setNavigationItemSelectedListener(this);

        TabLayout tabLayout = findViewById(R.id.main_tablayout);
        ViewPager viewPager = findViewById(R.id.main_viewpager);

        appFragment.setOperationCallback(this);
        importFragment.setOperationCallback(this);

        View view=LayoutInflater.from(this).inflate(R.layout.actionbar_search,null);
        final View cancelView=view.findViewById(R.id.actionbar_main_cancel);
        edit_search=view.findViewById(R.id.actionbar_main_edit);
        edit_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(s==null)return;
                cancelView.setVisibility(s.length()>0?View.VISIBLE:View.INVISIBLE);
                if(!isSearchMode)return;
                appFragment.updateSearchModeKeywords(s.toString());
                importFragment.updateSearchModeKeywords(s.toString());
            }
        });
        cancelView.setOnClickListener(v -> edit_search.setText(""));
        getSupportActionBar().setCustomView(view);

        viewPager.setAdapter(new MyPagerAdapter(this,getSupportFragmentManager(),appFragment,importFragment));
        tabLayout.setupWithViewPager(viewPager,true);
        viewPager.addOnPageChangeListener(this);

        if(PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }
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
        this.menu=menu;
        //setIconEnable(menu,true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if(id==android.R.id.home){
            checkAndExit();
        }

        if(id==R.id.action_search){
            openSearchMode();
        }

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
            }else if(currentSelection==1){
                final int mode_pak=settings.getInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT,Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT_DEFAULT);
                final int result_pak=mode_pak==0?1:0;
                editor.putInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT,result_pak);
                editor.apply();
                importFragment.setViewMode(result_pak);
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
            }else if(currentSelection==1){
                ImportItemSortConfigDialog importItemSortConfigDialog=new ImportItemSortConfigDialog(this, new SortConfigDialogCallback() {
                    @Override
                    public void onOptionSelected(int value) {
                       importFragment.sortGlobalListAndRefresh(value);
                    }
                });
                importItemSortConfigDialog.show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        drawerLayout.closeDrawers();
        switch (menuItem.getItemId()){
            default:break;
            case R.id.nav_receive:{
                if(Build.VERSION.SDK_INT>=23&&PermissionChecker.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    Global.showRequestingWritePermissionSnackBar(this);
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return false;
                }
                startActivityForResult(new Intent(this,FileReceiveActivity.class),REQUEST_CODE_RECEIVING_FILES);
            }
            break;
            case R.id.nav_settings:{
                startActivityForResult(new Intent(this,SettingActivity.class),REQUEST_CODE_SETTINGS);
            }
            break;
            case R.id.nav_about:{
                View dialogView=LayoutInflater.from(this).inflate(R.layout.dialog_about, null);
                dialogView.findViewById(R.id.layout_about_donate).setOnClickListener(v -> {
                    try{
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://qr.alipay.com/fkx1683000p6ngh3ifh1g6c")));
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                });
                new AlertDialog.Builder(this)
                        .setTitle(EnvironmentUtil.getAppName(this)+"("+EnvironmentUtil.getAppVersionName(this)+")")
                        .setIcon(R.drawable.icon_launcher)
                        .setCancelable(true)
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), (arg0, arg1) -> {}).show();
            }
            break;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            checkAndExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static final int REQUEST_CODE_SETTINGS=0;
    private static final int REQUEST_CODE_RECEIVING_FILES=1;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            default:break;
            case REQUEST_CODE_SETTINGS:{
                if(resultCode==RESULT_OK){
                    //recreate();
                    finish();
                    startActivity(new Intent(this,MainActivity.class));
                }
            }
            break;
            case REQUEST_CODE_RECEIVING_FILES:{
                if(resultCode==RESULT_OK){
                    sendBroadcast(new Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST));
                }
            }
            break;
        }
    }

    private void openSearchMode(){
        isSearchMode=true;
        setActionbarDisplayCustomView(true);
        setMenuVisible(false);
        appFragment.setSearchMode(true);
        importFragment.setSearchMode(true);
        EnvironmentUtil.showInputMethod(edit_search);
    }

    private void closeSearchMode(){
        isSearchMode=false;
        setMenuVisible(true);
        edit_search.setText("");
        setActionbarDisplayCustomView(false);
        appFragment.setSearchMode(false);
        importFragment.setSearchMode(false);
        EnvironmentUtil.hideInputMethod(this);
    }

    private void setMenuVisible(boolean b){
        if(menu==null)return;
        for(int i=0;i<menu.size();i++){
            menu.getItem(i).setVisible(b);
        }
    }

    private void setActionbarDisplayCustomView(boolean b){
        try{
            getSupportActionBar().setDisplayShowCustomEnabled(b);
        }catch (Exception e){e.printStackTrace();}
    }

    private void checkAndExit(){
        if(drawerLayout.isDrawerOpen(Gravity.START)){
            drawerLayout.closeDrawers();
            return;
        }
        switch(currentSelection){
            default:break;
            case 0:{
                if(appFragment.isMultiSelectMode()){
                    appFragment.closeMultiSelectMode();
                    return;
                }
            }
            break;
            case 1:{
                if(importFragment.isMultiSelectMode()){
                    importFragment.closeMultiSelectMode();
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
