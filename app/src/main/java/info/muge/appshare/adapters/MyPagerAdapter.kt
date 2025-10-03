package info.muge.appshare.adapters

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import info.muge.appshare.R

/**
 * ViewPager 适配器（Kotlin 版本）
 * 用于管理 ViewPager 中的 Fragment 页面
 *
 * 优化说明：
 * 1. 使用 Kotlin 实现，代码更简洁
 * 2. 使用 FragmentStatePagerAdapter 替代 FragmentPagerAdapter，更节省内存
 * 3. 使用 BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT 确保只有当前可见的 Fragment 处于 RESUMED 状态
 * 4. 支持动态添加/移除页面
 * 5. 改进空值检查和边界检查
 * 6. 支持自定义页面标题
 *
 * 注意：FragmentStatePagerAdapter 已被标记为废弃
 * 建议未来迁移到 ViewPager2 + FragmentStateAdapter
 * 详见：MyViewPager2Adapter.kt
 */
@Suppress("DEPRECATION")
class MyPagerAdapter(
    private val activity: Activity,
    fm: FragmentManager,
    vararg fragments: Fragment
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    // 使用可变列表存储 Fragment 和标题，支持动态修改
    private val fragments: MutableList<Fragment> = fragments.toMutableList()
    private val pageTitles: MutableList<String> = mutableListOf()

    init {
        // 初始化默认标题
        initDefaultTitles()
    }

    /**
     * 初始化默认页面标题
     */
    private fun initDefaultTitles() {
        pageTitles.clear()
        fragments.forEachIndexed { index, _ ->
            val title = when (index) {
                0 -> activity.resources.getString(R.string.main_page_export)
                else -> ""
            }
            pageTitles.add(title)
        }
    }

    override fun getItem(position: Int): Fragment {
        // 边界检查
        require(position in fragments.indices) {
            "Invalid position: $position, size: ${fragments.size}"
        }
        return fragments[position]
    }

    override fun getCount(): Int = fragments.size

    override fun getPageTitle(position: Int): CharSequence? {
        // 边界检查
        return if (position in pageTitles.indices) {
            pageTitles[position]
        } else {
            ""
        }
    }

    /**
     * 设置指定位置的页面标题
     *
     * @param position 页面位置
     * @param title 页面标题
     */
    fun setPageTitle(position: Int, title: String) {
        if (position in pageTitles.indices) {
            pageTitles[position] = title
            notifyDataSetChanged()
        }
    }

    /**
     * 添加新的页面
     *
     * @param fragment 要添加的 Fragment
     * @param title 页面标题
     */
    fun addPage(fragment: Fragment, title: String) {
        fragments.add(fragment)
        pageTitles.add(title)
        notifyDataSetChanged()
    }

    /**
     * 移除指定位置的页面
     *
     * @param position 要移除的页面位置
     */
    fun removePage(position: Int) {
        if (position in fragments.indices) {
            fragments.removeAt(position)
            pageTitles.removeAt(position)
            notifyDataSetChanged()
        }
    }

    /**
     * 获取指定位置的 Fragment
     *
     * @param position 页面位置
     * @return Fragment 实例，如果位置无效则返回 null
     */
    fun getFragment(position: Int): Fragment? {
        return if (position in fragments.indices) {
            fragments[position]
        } else {
            null
        }
    }

    /**
     * 清空所有页面
     */
    fun clear() {
        fragments.clear()
        pageTitles.clear()
        notifyDataSetChanged()
    }

    /**
     * 获取所有 Fragment
     */
    fun getAllFragments(): List<Fragment> = fragments.toList()

    /**
     * 获取所有页面标题
     */
    fun getAllTitles(): List<String> = pageTitles.toList()
}

