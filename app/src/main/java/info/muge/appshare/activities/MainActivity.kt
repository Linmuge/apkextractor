package info.muge.appshare.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.adapters.MyViewPager2Adapter
import info.muge.appshare.base.BaseActivity
import info.muge.appshare.databinding.ActivityMainBinding
import info.muge.appshare.fragments.AppFragment
import info.muge.appshare.fragments.OperationCallback
import info.muge.appshare.fragments.SettingsFragment
import info.muge.appshare.fragments.StatisticsFragment
import info.muge.appshare.ui.AppItemSortConfigDialog
import info.muge.appshare.ui.SortConfigDialogCallback
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.setStatusBarIconColorMode
import info.muge.appshare.utils.setupSystemBarInsets

/**
 * 主Activity
 * 包含应用列表、统计、设置三个页面
 * 使用底部导航栏进行页面切换
 */
class MainActivity : BaseActivity<ActivityMainBinding>(),
    View.OnClickListener,
    CompoundButton.OnCheckedChangeListener,
    OperationCallback {

    private val appFragment = AppFragment()
    private val statisticsFragment = StatisticsFragment()
    private val settingsFragment = SettingsFragment()
    private var currentSelection = 0
    private var isSearchMode = false
    private lateinit var pagerAdapter: MyViewPager2Adapter

    override fun ActivityMainBinding.initView() {
        // 设置边到边显示（Edge-to-Edge）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        appbar.setupSystemBarInsets(true, false)

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

        // 设置ViewPager2和底部导航栏
        setupViewPagerAndBottomNav()
    }

    /**
     * 设置 ViewPager2 和底部导航栏
     */
    private fun ActivityMainBinding.setupViewPagerAndBottomNav() {
        // 创建适配器，包含三个Fragment
        pagerAdapter = MyViewPager2Adapter(this@MainActivity, appFragment, statisticsFragment, settingsFragment)
        mainViewpager.adapter = pagerAdapter
        mainViewpager.isUserInputEnabled = false // 禁用滑动切换，使用底部导航栏切换

        // 注册页面变化回调
        mainViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentSelection = position
                updateToolbarTitle(position)
                updateSearchViewVisibility(position)
                updateMenuVisibility()
            }
        })

        // 设置底部导航栏
        bottomNavigation.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    mainViewpager.setCurrentItem(0, false)
                    return@OnItemSelectedListener true
                }
                R.id.nav_statistics -> {
                    mainViewpager.setCurrentItem(1, false)
                    return@OnItemSelectedListener true
                }
                R.id.nav_settings -> {
                    mainViewpager.setCurrentItem(2, false)
                    return@OnItemSelectedListener true
                }
            }
            false
        })
    }

    /**
     * 更新Toolbar标题
     */
    private fun updateToolbarTitle(position: Int) {
        supportActionBar?.title = when (position) {
            0 -> resources.getString(R.string.app_name)
            1 -> "统计"
            2 -> resources.getString(R.string.action_settings)
            else -> resources.getString(R.string.app_name)
        }
    }

    /**
     * 更新SearchView可见性
     */
    private fun updateSearchViewVisibility(position: Int) {
        binding.toolbar.findViewById<SearchView>(R.id.searchview).visibility =
            if (position == 0) View.VISIBLE else View.GONE
    }

    /**
     * 更新菜单可见性（仅在首页显示）
     */
    private fun updateMenuVisibility() {
        invalidateOptionsMenu()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // 只在首页显示菜单项
        menu.findItem(R.id.action_sort)?.isVisible = (currentSelection == 0)
        menu.findItem(R.id.action_view)?.isVisible = (currentSelection == 0)
        return true
    }

    /**
     * 设置搜索视图
     */
    private fun setupSearchView() {
        val searchView = binding.toolbar.findViewById<SearchView>(R.id.searchview)
        searchView.setOnSearchClickListener {
            if (currentSelection == 0) {
                openSearchMode()
            } else {
                searchView.isIconified = true
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (isSearchMode && currentSelection == 0) {
                    if (newText.isBlank()) {
                        appFragment.setSearchMode(false)
                    } else {
                        appFragment.updateSearchModeKeywords(newText)
                    }
                }
                return true
            }
        })

        searchView.setOnCloseListener {
            if (currentSelection == 0) {
                closeSearchMode()
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        // 确保底部导航栏状态正确
        updateBottomNavigationSelection()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // View.OnClickListener 实现
    override fun onClick(v: View) {}

    // CompoundButton.OnCheckedChangeListener 实现
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {}

    // OperationCallback 实现
    override fun onItemLongClickedAndMultiSelectModeOpened(fragment: Fragment) {}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_view -> {
                if (isSearchMode) return false
                if (currentSelection == 0) {
                    val settings = SPUtil.getGlobalSharedPreferences(this)
                    val editor = settings.edit()
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
        }

        return super.onOptionsItemSelected(item)
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

    /**
     * 更新底部导航栏选中状态
     */
    private fun updateBottomNavigationSelection() {
        when (currentSelection) {
            0 -> binding.bottomNavigation.selectedItemId = R.id.nav_home
            1 -> binding.bottomNavigation.selectedItemId = R.id.nav_statistics
            2 -> binding.bottomNavigation.selectedItemId = R.id.nav_settings
        }
    }

    companion object {
        private const val REQUEST_CODE_SETTINGS = 0
        private const val REQUEST_CODE_RECEIVING_FILES = 1
    }
}
