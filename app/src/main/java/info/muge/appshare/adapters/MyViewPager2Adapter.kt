package info.muge.appshare.adapters

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import info.muge.appshare.R

/**
 * ViewPager2 适配器（现代化版本）
 * 用于管理 ViewPager2 中的 Fragment 页面
 *
 * 优势：
 * 1. 使用最新的 ViewPager2 + FragmentStateAdapter，不会被废弃
 * 2. 支持垂直滚动
 * 3. 支持 RTL（从右到左）布局
 * 4. 更好的性能和内存管理
 * 5. 基于 RecyclerView 实现，支持 DiffUtil
 * 6. 支持动态添加/移除页面
 *
 * 使用方法：
 * 1. 在 build.gradle.kts 中添加依赖：
 *    implementation("androidx.viewpager2:viewpager2:1.1.0")
 *
 * 2. 在布局文件中使用 ViewPager2：
 *    <androidx.viewpager2.widget.ViewPager2
 *        android:id="@+id/main_viewpager"
 *        android:layout_width="match_parent"
 *        android:layout_height="match_parent" />
 *
 * 3. 在 Activity 中设置适配器：
 *    val adapter = MyViewPager2Adapter(this, appFragment)
 *    binding.mainViewpager.adapter = adapter
 *    TabLayoutMediator(binding.mainTablayout, binding.mainViewpager) { tab, position ->
 *        tab.text = adapter.getPageTitle(position)
 *    }.attach()
 *
 * 4. 监听页面变化：
 *    binding.mainViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
 *        override fun onPageSelected(position: Int) {
 *            // 处理页面选中事件
 *        }
 *    })
 */
class MyViewPager2Adapter(
    private val activity: FragmentActivity,
    vararg fragments: Fragment
) : FragmentStateAdapter(activity) {

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

    override fun createFragment(position: Int): Fragment {
        // 边界检查
        require(position in fragments.indices) {
            "Invalid position: $position, size: ${fragments.size}"
        }
        return fragments[position]
    }

    override fun getItemCount(): Int = fragments.size

    /**
     * 获取页面标题
     * 用于 TabLayoutMediator
     *
     * @param position 页面位置
     * @return 页面标题
     */
    fun getPageTitle(position: Int): CharSequence {
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
            notifyItemChanged(position)
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
        notifyItemInserted(fragments.size - 1)
    }

    /**
     * 在指定位置插入页面
     *
     * @param position 插入位置
     * @param fragment 要插入的 Fragment
     * @param title 页面标题
     */
    fun insertPage(position: Int, fragment: Fragment, title: String) {
        if (position in 0..fragments.size) {
            fragments.add(position, fragment)
            pageTitles.add(position, title)
            notifyItemInserted(position)
        }
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
            notifyItemRemoved(position)
        }
    }

    /**
     * 移动页面位置
     *
     * @param fromPosition 原位置
     * @param toPosition 目标位置
     */
    fun movePage(fromPosition: Int, toPosition: Int) {
        if (fromPosition in fragments.indices && toPosition in fragments.indices) {
            val fragment = fragments.removeAt(fromPosition)
            val title = pageTitles.removeAt(fromPosition)
            fragments.add(toPosition, fragment)
            pageTitles.add(toPosition, title)
            notifyItemMoved(fromPosition, toPosition)
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
        val size = fragments.size
        fragments.clear()
        pageTitles.clear()
        notifyItemRangeRemoved(0, size)
    }

    /**
     * 获取所有 Fragment
     */
    fun getAllFragments(): List<Fragment> = fragments.toList()

    /**
     * 获取所有页面标题
     */
    fun getAllTitles(): List<String> = pageTitles.toList()

    /**
     * 重写 getItemId 以支持动态数据变化
     * 这对于正确处理 Fragment 的生命周期很重要
     */
    override fun getItemId(position: Int): Long {
        return fragments[position].hashCode().toLong()
    }

    /**
     * 重写 containsItem 以支持动态数据变化
     */
    override fun containsItem(itemId: Long): Boolean {
        return fragments.any { it.hashCode().toLong() == itemId }
    }
}

