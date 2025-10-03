package info.muge.appshare.fragments

import androidx.fragment.app.Fragment

/**
 * 操作回调接口
 */
interface OperationCallback {
    /**
     * 当项目被长按并且多选模式被打开时调用
     * @param fragment 触发事件的Fragment
     */
    fun onItemLongClickedAndMultiSelectModeOpened(fragment: Fragment)
}

