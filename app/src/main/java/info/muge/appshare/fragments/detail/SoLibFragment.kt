package info.muge.appshare.fragments.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.databinding.FragmentComponentListBinding
import java.util.zip.ZipFile

/**
 * Native Libraries Viewer Fragment
 * Displays .so files found in the APK's lib/ directory
 */
class SoLibFragment : BaseDetailFragment() {

    private var _binding: FragmentComponentListBinding? = null
    private val binding get() = _binding!!
    
    private val libList = mutableListOf<SoLibItem>()
    private lateinit var adapter: SoLibAdapter

    companion object {
        fun newInstance(packageName: String): SoLibFragment {
            return SoLibFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PACKAGE_NAME, packageName)
                }
            }
        }
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
        
        // Setup RecyclerView
        adapter = SoLibAdapter(libList) { item -> copyToClipboard(item.name) }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        // Load Data
        loadData()
    }

    private fun loadData() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        
        Thread {
            val items = mutableListOf<SoLibItem>()
            
            appItem?.let { item ->
                try {
                    val sourcePath = item.getSourcePath()
                    if (sourcePath.isNotEmpty()) {
                        val zipFile = ZipFile(sourcePath)
                        val entries = zipFile.entries()
                        
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val name = entry.name
                            
                            // Check if it's a library file
                            if (name.startsWith("lib/") && name.endsWith(".so")) {
                                // format: lib/<arch>/<filename.so>
                                val parts = name.split("/")
                                if (parts.size >= 3) {
                                    val arch = parts[1]
                                    val fileName = parts.last()
                                    items.add(SoLibItem(fileName, arch, name))
                                }
                            }
                        }
                        zipFile.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Sort: Architecture then Filename
            items.sortWith(compareBy({ it.arch }, { it.name }))
            
            Global.handler.post {
                if (_binding != null) {
                    libList.clear()
                    libList.addAll(items)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class SoLibItem(
    val name: String,
    val arch: String,
    val fullPath: String
)

class SoLibAdapter(
    private val items: List<SoLibItem>,
    private val onItemClick: (SoLibItem) -> Unit
) : RecyclerView.Adapter<SoLibAdapter.ViewHolder>() {

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
        holder.subtitleView.visibility = View.VISIBLE
        holder.subtitleView.text = item.arch
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
