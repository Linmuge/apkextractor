package info.muge.appshare.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.muge.appshare.R
import info.muge.appshare.activities.AppDetailActivity
import info.muge.appshare.items.AppItem

/**
 * 统计页面应用列表底部弹窗
 */
class StatisticsAppListBottomSheet : BottomSheetDialogFragment() {

    private var title: String = ""
    private var apps: List<AppItem> = listOf()

    private lateinit var titleText: TextView
    private lateinit var countText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter

    companion object {
        fun newInstance(title: String, apps: List<AppItem>): StatisticsAppListBottomSheet {
            return StatisticsAppListBottomSheet().apply {
                this.title = title
                this.apps = apps
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleText = view.findViewById(R.id.bottom_sheet_title)
        countText = view.findViewById(R.id.bottom_sheet_count)
        recyclerView = view.findViewById(R.id.bottom_sheet_recycler)

        titleText.text = title
        countText.text = "共 ${apps.size} 个"

        adapter = AppListAdapter { app ->
            val intent = Intent(requireContext(), AppDetailActivity::class.java).apply {
                putExtra("package_name", app.getPackageName())
            }
            startActivity(intent)
            dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        adapter.submitList(apps)
    }

    /**
     * 简单的应用列表适配器
     */
    private class AppListAdapter(
        private val onItemClicked: (AppItem) -> Unit
    ) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

        private var items: List<AppItem> = listOf()

        fun submitList(newItems: List<AppItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val root: View = view.findViewById(R.id.item_app_root)
            val icon: android.widget.ImageView = view.findViewById(R.id.item_app_icon)
            val title: TextView = view.findViewById(R.id.item_app_title)
            val description: TextView = view.findViewById(R.id.item_app_description)
            val rightText: TextView = view.findViewById(R.id.item_app_right)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_info_linear, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.icon.setImageDrawable(item.getIconDrawable())
            holder.title.text = item.getTitle()
            holder.description.text = item.getPackageName()
            holder.rightText.text = android.text.format.Formatter.formatFileSize(
                holder.itemView.context,
                item.getSize()
            )

            // 设置系统应用标题颜色
            val appInfo = item.getPackageInfo().applicationInfo
            val titleColor = if (appInfo != null && (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0)) {
                androidx.core.content.ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.colorSystemAppTitleColor
                )
            } else {
                androidx.core.content.ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.colorHighLightText
                )
            }
            holder.title.setTextColor(titleColor)

            holder.root.setOnClickListener {
                onItemClicked(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
