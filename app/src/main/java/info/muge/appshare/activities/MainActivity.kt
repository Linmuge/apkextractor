package info.muge.appshare.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.PermissionChecker
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.adapters.MyViewPager2Adapter
import info.muge.appshare.base.BaseActivity
import info.muge.appshare.databinding.ActivityMainBinding
import info.muge.appshare.fragments.AppFragment
import info.muge.appshare.fragments.OperationCallback
import info.muge.appshare.ui.AppItemSortConfigDialog
import info.muge.appshare.ui.SortConfigDialogCallback
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.setStatusBarIconColorMode
import info.muge.appshare.utils.setupSystemBarInsets

/**
 * 主Activity
 * 包含应用列表、搜索功能、设置等
 *
 * 已升级到 ViewPager2，提供更好的性能和功能
 */
class MainActivity : BaseActivity<ActivityMainBinding>(),
    View.OnClickListener,
    CompoundButton.OnCheckedChangeListener,
    OperationCallback {

    private val appFragment = AppFragment()
    private var currentSelection = 0
    private var isSearchMode = false
    private lateinit var pagerAdapter: MyViewPager2Adapter
    private var tabLayoutMediator: TabLayoutMediator? = null

    override fun ActivityMainBinding.initView() {
        // 设置边到边显示（Edge-to-Edge）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        appbar.setupSystemBarInsets(true,false)

        // 禁用 AppBarLayout 的 lift 行为（防止滚动时状态栏变色）
        appbar.setLiftable(false)
        appbar.isLiftOnScroll = false

        // 设置状态栏背景色为透明（防止滚动时变色）
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // 禁用状态栏对比度强制（防止系统自动改变状态栏颜色）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        // 设置Toolbar
        setSupportActionBar(toolbar)
        try {
            supportActionBar?.setTitle(resources.getString(R.string.app_name))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 设置Fragment回调
        appFragment.setOperationCallback(this@MainActivity)

        // 设置SearchView
        setupSearchView()

        // 设置ViewPager2和TabLayout
        setupViewPager()
    }

    /**
     * 设置 ViewPager2 和 TabLayout
     */
    private fun ActivityMainBinding.setupViewPager() {
        // 创建适配器
        pagerAdapter = MyViewPager2Adapter(this@MainActivity, appFragment)
        mainViewpager.adapter = pagerAdapter

        // 使用 TabLayoutMediator 连接 TabLayout 和 ViewPager2
        tabLayoutMediator = TabLayoutMediator(mainTablayout, mainViewpager) { tab, position ->
            tab.text = pagerAdapter.getPageTitle(position)
        }
        tabLayoutMediator?.attach()

        // 注册页面变化回调
        mainViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentSelection = position
            }
        })
    }

    /**
     * 设置搜索视图
     */
    private fun ActivityMainBinding.setupSearchView() {
        searchview.setOnSearchClickListener {
            openSearchMode()
        }
        
        searchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (isSearchMode) {
                    if (newText.isBlank()) {
                        appFragment.setSearchMode(false)
                    } else {
                        appFragment.updateSearchModeKeywords(newText)
                    }
                }
                return true
            }
        })
        
        searchview.setOnCloseListener {
            closeSearchMode()
            false
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理 TabLayoutMediator，避免内存泄漏
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
    }

    // View.OnClickListener 实现
    override fun onClick(v: View) {}

    // CompoundButton.OnCheckedChangeListener 实现
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {}

    // OperationCallback 实现
    override fun onItemLongClickedAndMultiSelectModeOpened(fragment: Fragment) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isEmpty()) return
            if (grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                sendBroadcast(Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_view -> {
                if (isSearchMode) return false
                val settings = SPUtil.getGlobalSharedPreferences(this)
                val editor = settings.edit()
                if (currentSelection == 0) {
                    val modeApp = settings.getInt(
                        Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,
                        Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT
                    )
                    val resultApp = if (modeApp == 0) 1 else 0
                    editor.putInt(Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE, resultApp)
                    editor.apply()
                    appFragment.setViewMode(resultApp)
                }
            }
            
            R.id.action_sort -> {
                if (currentSelection == 0) {
                    val dialog = AppItemSortConfigDialog(this, object : SortConfigDialogCallback {
                        override fun onOptionSelected(value: Int) {
                            appFragment.sortGlobalListAndRefresh(value)
                        }
                    })
                    dialog.show()
                }
            }
            
            R.id.action_settings -> {
                startActivityForResult(
                    Intent(this, SettingActivity::class.java),
                    REQUEST_CODE_SETTINGS
                )
            }
            
            R.id.action_about -> {
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
                MaterialAlertDialogBuilder(
                    this,
                    com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                )
                    .setTitle("${EnvironmentUtil.getAppName(this)}(${EnvironmentUtil.getAppVersionName(this)})")
                    .setIcon(R.mipmap.ic_launcher_round)
                    .setCancelable(true)
                    .setView(dialogView)
                    .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                    .show()
            }
        }
        
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SETTINGS -> {
                if (resultCode == RESULT_OK) {
                    finish()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
            REQUEST_CODE_RECEIVING_FILES -> {
                if (resultCode == RESULT_OK) {
                    sendBroadcast(Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST))
                }
            }
        }
    }

    /**
     * 打开搜索模式
     */
    private fun openSearchMode() {
        isSearchMode = true
        appFragment.setSearchMode(true)
    }

    /**
     * 关闭搜索模式
     */
    private fun closeSearchMode() {
        isSearchMode = false
        appFragment.setSearchMode(false)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            checkAndExit()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 检查并退出应用
     * 处理多选模式、搜索模式等状态
     */
    private fun checkAndExit() {
        when (currentSelection) {
            0 -> {
                if (appFragment.isMultiSelectMode) {
                    appFragment.closeMultiSelectMode()
                    return
                }
            }
        }

        if (isSearchMode) {
            closeSearchMode()
            return
        }

        finish()
    }

    companion object {
        private const val REQUEST_CODE_SETTINGS = 0
        private const val REQUEST_CODE_RECEIVING_FILES = 1
    }
}

