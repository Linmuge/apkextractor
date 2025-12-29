package info.muge.appshare.fragments.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.databinding.FragmentComponentListBinding
import info.muge.appshare.ui.ToastManager

/**
 * 组件列表类型枚举
 */
enum class ComponentType {
    PERMISSION,
    ACTIVITY,
    SERVICE,
    RECEIVER,
    PROVIDER,
    STATIC_LOADER
}

/**
 * 通用组件列表 Fragment
 * 用于显示权限、Activity、Service、Receiver、Provider、静态接收器列表
 */
class ComponentListFragment : BaseDetailFragment() {

    private var _binding: FragmentComponentListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var componentType: ComponentType
    private val componentList = mutableListOf<ComponentItem>()
    private lateinit var adapter: ComponentAdapter

    companion object {
        private const val ARG_COMPONENT_TYPE = "component_type"
        
        fun newInstance(packageName: String, type: ComponentType): ComponentListFragment {
            return ComponentListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PACKAGE_NAME, packageName)
                    putString(ARG_COMPONENT_TYPE, type.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        componentType = ComponentType.valueOf(
            arguments?.getString(ARG_COMPONENT_TYPE) ?: ComponentType.PERMISSION.name
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComponentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 设置 RecyclerView
        adapter = ComponentAdapter(
            componentList,
            onItemClick = { item -> copyToClipboard(item.name) },
            onItemLongClick = { item -> handleLongClick(item) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        // 加载数据
        loadData()
    }

    private fun loadData() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        
        Thread {
            val items = mutableListOf<ComponentItem>()
            
            appItem?.let { item ->
                val packageInfo = item.getPackageInfo()
                
                when (componentType) {
                    ComponentType.PERMISSION -> {
                        packageInfo.requestedPermissions?.forEach { permission ->
                            if (permission != null) {
                                items.add(ComponentItem(permission, null, false))
                            }
                        }
                    }
                    ComponentType.ACTIVITY -> {
                        packageInfo.activities?.forEach { activityInfo ->
                            items.add(ComponentItem(
                                activityInfo.name,
                                activityInfo.packageName,
                                true
                            ))
                        }
                    }
                    ComponentType.SERVICE -> {
                        packageInfo.services?.forEach { serviceInfo ->
                            items.add(ComponentItem(
                                serviceInfo.name,
                                serviceInfo.packageName,
                                true
                            ))
                        }
                    }
                    ComponentType.RECEIVER -> {
                        packageInfo.receivers?.forEach { receiverInfo ->
                            items.add(ComponentItem(receiverInfo.name, null, false))
                        }
                    }
                    ComponentType.PROVIDER -> {
                        packageInfo.providers?.forEach { providerInfo ->
                            items.add(ComponentItem(providerInfo.name, null, false))
                        }
                    }
                    ComponentType.STATIC_LOADER -> {
                        val bundle = item.getStaticReceiversBundle()
                        bundle.keySet().forEach { key ->
                            val filters = bundle.getStringArrayList(key)
                            val description = filters?.joinToString(", ") ?: ""
                            items.add(ComponentItem(key, description, false))
                        }
                    }
                }
            }
            
            Global.handler.post {
                if (_binding != null) {
                    componentList.clear()
                    componentList.addAll(items)
                    adapter.notifyDataSetChanged()
                    
                    binding.loadingProgress.visibility = View.GONE
                    if (items.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }.start()
    }

    private fun handleLongClick(item: ComponentItem): Boolean {
        val packageName = item.packageName ?: return false
        
        return when (componentType) {
            ComponentType.ACTIVITY -> {
                try {
                    val intent = Intent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.setClassName(packageName, item.name)
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    ToastManager.showToast(requireContext(), e.toString(), Toast.LENGTH_SHORT)
                    false
                }
            }
            ComponentType.SERVICE -> {
                try {
                    val intent = Intent()
                    intent.setClassName(packageName, item.name)
                    requireContext().startService(intent)
                    true
                } catch (e: Exception) {
                    ToastManager.showToast(requireContext(), e.toString(), Toast.LENGTH_SHORT)
                    false
                }
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * 组件项数据类
 */
data class ComponentItem(
    val name: String,
    val packageName: String?,  // 用于启动 Activity/Service
    val canLaunch: Boolean
)

/**
 * 组件列表适配器
 */
class ComponentAdapter(
    private val items: List<ComponentItem>,
    private val onItemClick: (ComponentItem) -> Unit,
    private val onItemLongClick: (ComponentItem) -> Boolean
) : RecyclerView.Adapter<ComponentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: android.widget.TextView = view.findViewById(R.id.item_textview)
        val subtitleView: android.widget.TextView = view.findViewById(R.id.item_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_component_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item.name
        
        // 显示提示文字
        if (item.canLaunch) {
            holder.subtitleView.visibility = View.VISIBLE
            holder.subtitleView.text = "点击复制 · 长按启动"
            holder.itemView.setOnLongClickListener { onItemLongClick(item) }
        } else {
            holder.subtitleView.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

