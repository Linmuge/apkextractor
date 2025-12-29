package info.muge.appshare.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import info.muge.appshare.R
import info.muge.appshare.fragments.detail.AppInfoFragment
import info.muge.appshare.fragments.detail.ComponentListFragment
import info.muge.appshare.fragments.detail.ComponentType
import info.muge.appshare.fragments.detail.HashFragment
import info.muge.appshare.fragments.detail.SignatureFragment

/**
 * 应用详情页 ViewPager2 适配器
 * 管理 9 个 Tab 页面的 Fragment
 */
class AppDetailPagerAdapter(
    activity: FragmentActivity,
    private val packageName: String
) : FragmentStateAdapter(activity) {

    companion object {
        const val TAB_COUNT = 9
        
        const val TAB_APP_INFO = 0
        const val TAB_SIGNATURE = 1
        const val TAB_HASH = 2
        const val TAB_PERMISSIONS = 3
        const val TAB_ACTIVITIES = 4
        const val TAB_SERVICES = 5
        const val TAB_RECEIVERS = 6
        const val TAB_PROVIDERS = 7
        const val TAB_STATIC_LOADERS = 8
    }

    private val tabTitles = intArrayOf(
        R.string.tab_app_info,
        R.string.tab_signature,
        R.string.tab_hash,
        R.string.tab_permissions,
        R.string.tab_activities,
        R.string.tab_services,
        R.string.tab_receivers,
        R.string.tab_providers,
        R.string.tab_static_loaders
    )

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_APP_INFO -> AppInfoFragment.newInstance(packageName)
            TAB_SIGNATURE -> SignatureFragment.newInstance(packageName)
            TAB_HASH -> HashFragment.newInstance(packageName)
            TAB_PERMISSIONS -> ComponentListFragment.newInstance(packageName, ComponentType.PERMISSION)
            TAB_ACTIVITIES -> ComponentListFragment.newInstance(packageName, ComponentType.ACTIVITY)
            TAB_SERVICES -> ComponentListFragment.newInstance(packageName, ComponentType.SERVICE)
            TAB_RECEIVERS -> ComponentListFragment.newInstance(packageName, ComponentType.RECEIVER)
            TAB_PROVIDERS -> ComponentListFragment.newInstance(packageName, ComponentType.PROVIDER)
            TAB_STATIC_LOADERS -> ComponentListFragment.newInstance(packageName, ComponentType.STATIC_LOADER)
            else -> AppInfoFragment.newInstance(packageName)
        }
    }

    /**
     * 获取 Tab 标题资源 ID
     */
    fun getTabTitleResId(position: Int): Int {
        return if (position in tabTitles.indices) {
            tabTitles[position]
        } else {
            R.string.tab_app_info
        }
    }
}
