package info.muge.appshare.fragments

import android.content.*
import android.graphics.drawable.ColorDrawable
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.snackbar.Snackbar
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.Global.ExportTaskFinishedListener
import info.muge.appshare.R
import info.muge.appshare.activities.AppDetailActivity
import info.muge.appshare.activities.BaseActivity
import info.muge.appshare.adapters.RecyclerViewAdapter
import info.muge.appshare.adapters.RecyclerViewAdapter.ListAdapterOperationListener
import info.muge.appshare.base.BaseFragment
import info.muge.appshare.base.MainChildFragment
import info.muge.appshare.databinding.PageExportBinding
import info.muge.appshare.items.AppItem
import info.muge.appshare.tasks.RefreshInstalledListTask
import info.muge.appshare.tasks.RefreshInstalledListTask.RefreshInstalledListTaskCallback
import info.muge.appshare.tasks.SearchAppItemTask
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.StorageUtil
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import java.util.*

class AppFragment : BaseFragment<PageExportBinding>(), View.OnClickListener, RefreshInstalledListTaskCallback,
    ListAdapterOperationListener<AppItem>, SearchAppItemTask.SearchTaskCompletedCallback,
    MainChildFragment {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private var adapter: RecyclerViewAdapter<AppItem>? = null
    private lateinit var loading_content: ViewGroup
    private lateinit var progressBar: ProgressBar
    private lateinit var tv_progress: TextView
    private lateinit var viewGroup_no_content: ViewGroup
    private lateinit var card_multi_select: MaterialCardView
    private lateinit var cb_sys: MaterialCheckBox
    private lateinit var tv_space_remaining: TextView
    private lateinit var tv_multi_select_head: TextView
    private lateinit var btn_select_all: Button
    private lateinit var btn_export: Button
    private lateinit var btn_more: Button
    private var popupWindow: PopupWindow? = null
    private var isScrollable = false
    private var isSearchMode = false
    private var callback: OperationCallback? = null
    private var refreshInstalledListTask: RefreshInstalledListTask? = null
    val onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val isMultiSelectMode = adapter?.isMultiSelectMode?:false
            if (!recyclerView.canScrollVertically(-1)) {
                // onScrolledToTop();
            } else if (!recyclerView.canScrollVertically(1)) {
                // onScrolledToBottom();
                if (isMultiSelectMode) {
                    if (isScrollable && card_multi_select!!.visibility != View.GONE) setViewVisibilityWithAnimation(
                        card_multi_select,
                        View.GONE
                    )
                } else if (isScrollable && binding.exportCard.visibility != View.GONE && !isSearchMode) setViewVisibilityWithAnimation(
                    binding.exportCard,
                    View.GONE
                )
            } else if (dy < 0) {
                // onScrolledUp();
                if (isMultiSelectMode) {
                    if (isScrollable && card_multi_select!!.visibility != View.VISIBLE) setViewVisibilityWithAnimation(
                        card_multi_select,
                        View.VISIBLE
                    )
                } else if (isScrollable && binding.exportCard.visibility != View.VISIBLE && !isSearchMode) setViewVisibilityWithAnimation(
                    binding.exportCard,
                    View.VISIBLE
                )
            } else if (dy > 0) {
                // onScrolledDown();
                isScrollable = true
                if (isMultiSelectMode) {
                    if (card_multi_select!!.visibility != View.GONE) setViewVisibilityWithAnimation(
                        card_multi_select,
                        View.GONE
                    )
                } else if (binding.exportCard.visibility != View.GONE && !isSearchMode) setViewVisibilityWithAnimation(
                    binding.exportCard,
                    View.GONE
                )
            }
        }
    }
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (activity == null) return
            if (Constants.ACTION_REFRESH_APP_LIST.equals(intent.action, ignoreCase = true)) {
                setAndStartRefreshingTask()
            } else if (Constants.ACTION_REFRESH_AVAILIBLE_STORAGE.equals(
                    intent.action,
                    ignoreCase = true
                )
            ) {
                refreshAvailableStorage()
            }
        }
    }
    private val receiver_app: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (activity == null) return
            if (Intent.ACTION_PACKAGE_ADDED == intent.action || Intent.ACTION_PACKAGE_REMOVED == intent.action || Intent.ACTION_PACKAGE_REPLACED == intent.action) {
                setAndStartRefreshingTask()
            }
        }
    }
    override fun PageExportBinding.initView() {
        swipeRefreshLayout = pageContent.contentSwipe
        recyclerView = pageContent.contentRecyclerview
        loading_content = pageContent.loading
        progressBar = pageContent.loadingPg
        tv_progress = pageContent.loadingText
        viewGroup_no_content = pageContent.noContentAtt
        card_multi_select = binding.exportCardMultiSelect
        cb_sys = mainShowSystemApp
        tv_space_remaining = mainStorageRemain
        tv_multi_select_head = mainSelectNumSize
        btn_select_all = mainSelectAll
        btn_export = mainExport
        btn_more = mainMore
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.pp_more, null)
        val more_copy_package_names =
            popupView.findViewById<ViewGroup>(R.id.popup_copy_package_name)
        more_copy_package_names.setOnClickListener(this@AppFragment)
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow!!.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.color_popup_window)))
        popupWindow!!.isTouchable = true
        popupWindow!!.isOutsideTouchable = true
        try {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Constants.ACTION_REFRESH_APP_LIST)
            intentFilter.addAction(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST)
            intentFilter.addAction(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE)
            if (activity != null) requireActivity().registerReceiver(receiver, intentFilter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
            intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
            intentFilter.addDataScheme("package")
            if (activity != null) requireActivity().registerReceiver(receiver_app, intentFilter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        init()
    }
    private fun init() {
        if (activity == null) return
        cb_sys!!.isChecked = SPUtil.getGlobalSharedPreferences(requireActivity()).getBoolean(
            Constants.PREFERENCE_SHOW_SYSTEM_APP,
            Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT
        )
        swipeRefreshLayout!!.setColorSchemeColors(requireActivity().resources.getColor(R.color.colorTitle))
        btn_select_all!!.setOnClickListener(this)
        btn_export!!.setOnClickListener(this)
        btn_more!!.setOnClickListener(this)
        recyclerView!!.addOnScrollListener(onScrollListener)
        swipeRefreshLayout!!.setOnRefreshListener(OnRefreshListener {
            if (activity == null) return@OnRefreshListener
            if (isSearchMode) {
                swipeRefreshLayout!!.isRefreshing = false
                return@OnRefreshListener
            }
            if (adapter != null && adapter!!.isMultiSelectMode) {
                swipeRefreshLayout!!.isRefreshing = false
                return@OnRefreshListener
            }
            setAndStartRefreshingTask()
        })
        cb_sys!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            buttonView.isEnabled = false
            if (activity == null) return@OnCheckedChangeListener
            SPUtil.getGlobalSharedPreferences(requireActivity()).edit()
                .putBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP, isChecked).apply()
            setAndStartRefreshingTask()
        })
        setAndStartRefreshingTask()
        refreshAvailableStorage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (activity != null) requireActivity().unregisterReceiver(receiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (activity != null) requireActivity().unregisterReceiver(receiver_app)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(v: View) {
        if (activity == null) return
        when (v.id) {
            R.id.main_select_all -> {
                if (adapter != null) adapter!!.setToggleSelectAll()
            }
            R.id.main_export -> {
                if (adapter == null) return
                val arrayList = ArrayList(
                    adapter!!.selectedItems
                )
                Global.checkAndExportCertainAppItemsToSetPathWithoutShare(
                    requireActivity(), arrayList, true, ExportTaskFinishedListener { error_message ->
                        if (activity == null) return@ExportTaskFinishedListener
                        if (error_message.trim { it <= ' ' } != "") {
                            AlertDialog.Builder(requireActivity())
                                .setTitle(resources.getString(R.string.exception_title))
                                .setMessage(resources.getString(R.string.exception_message) + error_message)
                                .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { dialog, which -> }
                                .show()
                        } else {
                            ToastManager.showToast(
                                requireActivity(),
                                resources.getString(R.string.toast_export_complete) + " "
                                        + SPUtil.getDisplayingExportPath(),
                                Toast.LENGTH_SHORT
                            )
                        }
                        closeMultiSelectMode()
                        refreshAvailableStorage()
                    })
            }
            R.id.main_more -> {
                val appItemList = adapter!!.selectedItems
                if (appItemList.size == 0) {
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        resources.getString(R.string.snack_bar_no_app_selected),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return
                }
                popupWindow!!.dismiss()
                val stringBuilder = StringBuilder()
                for (appItem in appItemList) {
                    if (stringBuilder.toString().length > 0) stringBuilder.append(
                        SPUtil.getGlobalSharedPreferences(
                            requireActivity()
                        )
                            .getString(
                                Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR,
                                Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT
                            )
                    )
                    stringBuilder.append(appItem.packageName)
                }
                //closeMultiSelectMode();
                clip2ClipboardAndShowSnackbar(stringBuilder.toString())
            }
            else -> {}
        }
    }

    override fun onRefreshProgressStarted(total: Int) {
        if (activity == null) return
        isScrollable = false
        recyclerView!!.adapter = null
        loading_content!!.visibility = View.VISIBLE
        viewGroup_no_content!!.visibility = View.GONE
        progressBar!!.max = total
        progressBar!!.progress = 0
        swipeRefreshLayout!!.isRefreshing = true
        cb_sys!!.isEnabled = false
        card_multi_select!!.visibility = View.GONE
    }

    override fun onRefreshProgressUpdated(current: Int, total: Int) {
        if (activity == null) return
        progressBar!!.progress = current
        tv_progress!!.text =
            requireActivity().resources.getString(R.string.dialog_loading_title) + " " + current + "/" + total
    }

    override fun onRefreshCompleted(appList: List<AppItem>) {
        if (activity == null) return
        loading_content!!.visibility = View.GONE
        viewGroup_no_content!!.visibility = if (appList.size == 0) View.VISIBLE else View.GONE
        swipeRefreshLayout!!.isRefreshing = false
        swipeRefreshLayout!!.isEnabled = true
        adapter = RecyclerViewAdapter(
            requireActivity(),
            recyclerView!!,
            appList,
            SPUtil.getGlobalSharedPreferences(requireActivity()).getInt(
                Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE,
                Constants.PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT
            ),
            this
        )
        recyclerView!!.adapter = adapter
        cb_sys!!.isEnabled = true
        //if(isSearchMode)adapter.setData(null);
    }

    override fun onItemClicked(
        appItem: AppItem,
        viewHolder: RecyclerViewAdapter.ViewHolder,
        position: Int
    ) {
        if (activity == null) return
        val intent = Intent(activity, AppDetailActivity::class.java)
        intent.putExtra(BaseActivity.EXTRA_PACKAGE_NAME, appItem.packageName)
        val compat = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            Pair(viewHolder.icon, "icon")
        )
        try {
            ActivityCompat.startActivity(requireActivity(), intent, compat.toBundle())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMultiSelectItemChanged(selected_items: List<AppItem>, length: Long) {
        if (activity == null) return
        if (selected_items.isEmpty()){
            closeMultiSelectMode()
        }else{
            tv_multi_select_head.text =
                selected_items.size.toString() + resources.getString(R.string.unit_item) + "/" + Formatter.formatFileSize(
                    activity,
                    length
                )
            btn_export.isEnabled = selected_items.size > 0
        }
    }

    override fun onMultiSelectModeOpened() {
        if (activity == null) return
        binding.exportCard.visibility = View.GONE
        setViewVisibilityWithAnimation(card_multi_select, View.VISIBLE)
        //isCurrentPageMultiSelectMode=true;
        swipeRefreshLayout.isEnabled = false
        //setBackButtonVisible(true);
        EnvironmentUtil.hideInputMethod(requireActivity())
        if (callback != null) callback!!.onItemLongClickedAndMultiSelectModeOpened(this)
    }

    override fun onSearchTaskCompleted(appItems: List<AppItem>, keyword: String) {
        if (activity == null) return
        if (adapter == null) return
        swipeRefreshLayout!!.isRefreshing = false
        adapter!!.setData(appItems)
        adapter!!.setHighlightKeyword(keyword)
    }

    fun setOperationCallback(callback: OperationCallback?) {
        this.callback = callback
    }

    val isMultiSelectMode: Boolean
        get() = adapter != null && adapter!!.isMultiSelectMode

    fun closeMultiSelectMode() {
        if (adapter == null) return
        adapter!!.setMultiSelectMode(false)
        swipeRefreshLayout!!.isEnabled = true
        if (isSearchMode) {
            binding.exportCard.visibility = View.GONE
            setViewVisibilityWithAnimation(card_multi_select, View.GONE)
        } else {
            setViewVisibilityWithAnimation(binding.exportCard, View.VISIBLE)
            card_multi_select!!.visibility = View.GONE
        }
    }

    fun setSearchMode(b: Boolean) {
        isSearchMode = b
        if (b) {
            binding.exportCard.visibility = View.GONE
        } else {
            setViewVisibilityWithAnimation(binding.exportCard, View.VISIBLE)
            if (adapter != null) adapter!!.setHighlightKeyword(null)
        }
        card_multi_select.visibility = View.GONE
        swipeRefreshLayout.isEnabled = !b
        if (adapter == null) return
        adapter!!.setMultiSelectMode(false)
        if (b) {
            adapter!!.setData(null)
        } else {
            synchronized(Global.app_list) { adapter!!.setData(Global.app_list) }
        }
    }

    fun getIsSearchMode(): Boolean {
        return isSearchMode
    }

    private var searchAppItemTask: SearchAppItemTask? = null
    fun updateSearchModeKeywords(key: String) {
        if (activity == null) return
        if (adapter == null) return
        if (searchAppItemTask != null) searchAppItemTask!!.setInterrupted()
        synchronized(Global.app_list) {
            searchAppItemTask = SearchAppItemTask(Global.app_list, key, this)
        }
        adapter!!.setData(null)
        adapter!!.setMultiSelectMode(false)
        card_multi_select!!.visibility = View.GONE
        swipeRefreshLayout!!.isRefreshing = true
        searchAppItemTask!!.start()
    }

    fun sortGlobalListAndRefresh(value: Int) {
        closeMultiSelectMode()
        AppItem.sort_config = value
        if (adapter != null) adapter!!.setData(null)
        swipeRefreshLayout!!.isRefreshing = true
        cb_sys!!.isEnabled = false
        Thread {
            synchronized(Global.app_list) { Collections.sort(Global.app_list) }
            Global.handler.post {
                if (adapter != null) {
                    synchronized(Global.app_list) { adapter!!.setData(Global.app_list) }
                }
                swipeRefreshLayout!!.isRefreshing = false
                cb_sys!!.isEnabled = true
            }
        }.start()
    }

    fun setViewMode(mode: Int) {
        if (adapter == null) return
        adapter!!.setLayoutManagerAndView(mode)
    }

    private fun setAndStartRefreshingTask() {
        if (activity == null) return
        if (refreshInstalledListTask != null) refreshInstalledListTask!!.setInterrupted()
        refreshInstalledListTask = RefreshInstalledListTask(requireActivity(), this)
        swipeRefreshLayout!!.isRefreshing = true
        recyclerView!!.adapter = null
        cb_sys!!.isEnabled = false
        refreshInstalledListTask!!.start()
    }

    private fun setViewVisibilityWithAnimation(view: View, visibility: Int) {
        if (activity == null) return
        if (visibility == View.GONE) {
            if (view.visibility != View.GONE) view.startAnimation(
                AnimationUtils.loadAnimation(
                    activity, R.anim.exit_300
                )
            )
            view.visibility = View.GONE
        } else if (visibility == View.VISIBLE) {
            if (view.visibility != View.VISIBLE) view.startAnimation(
                AnimationUtils.loadAnimation(
                    activity, R.anim.entry_300
                )
            )
            view.visibility = View.VISIBLE
        }
    }

    private fun refreshAvailableStorage() {
        try {
            if (activity == null) return
            var head: String? = resources.getString(R.string.main_card_remaining_storage) + ":"
            val isExternal = SPUtil.getIsSaved2ExternalStorage(requireActivity())
            if (isExternal) {
                var available: Long = 0
                val files = requireActivity().getExternalFilesDirs(null)
                for (file in files) {
                    if (file.absolutePath.lowercase(Locale.getDefault())
                            .startsWith(StorageUtil.getMainExternalStoragePath())
                    ) continue
                    available = StorageUtil.getAvaliableSizeOfPath(file.absolutePath)
                }
                head += Formatter.formatFileSize(activity, available)
            } else {
                head += Formatter.formatFileSize(
                    activity,
                    StorageUtil.getAvaliableSizeOfPath(StorageUtil.getMainExternalStoragePath())
                )
            }
            tv_space_remaining!!.text = head
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clip2ClipboardAndShowSnackbar(s: String) {
        try {
            if (activity == null) return
            val manager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("message", s))
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                resources.getString(R.string.snack_bar_clipboard),
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override val name: String
        get() = "首页"
    override val fragment: Fragment
        get() = AppFragment()

}