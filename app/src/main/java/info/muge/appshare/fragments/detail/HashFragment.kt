package info.muge.appshare.fragments.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import info.muge.appshare.databinding.FragmentHashBinding
import info.muge.appshare.tasks.HashTask

/**
 * 文件哈希 Fragment
 * 显示 APK 文件的 MD5、SHA1、SHA256、CRC32 哈希值
 */
class HashFragment : BaseDetailFragment() {

    private var _binding: FragmentHashBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(packageName: String): HashFragment {
            return HashFragment().apply {
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
        _binding = FragmentHashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        appItem?.let { item ->
            val fileItem = item.getFileItem()
            
            // 加载 MD5
            HashTask(fileItem, HashTask.HashType.MD5, object : HashTask.CompletedCallback {
                override fun onHashCompleted(result: String) {
                    if (_binding != null) {
                        binding.hashMd5Progress.visibility = View.GONE
                        binding.hashMd5Value.visibility = View.VISIBLE
                        binding.hashMd5Value.text = result
                    }
                }
            }).start()
            
            // 加载 SHA1
            HashTask(fileItem, HashTask.HashType.SHA1, object : HashTask.CompletedCallback {
                override fun onHashCompleted(result: String) {
                    if (_binding != null) {
                        binding.hashSha1Progress.visibility = View.GONE
                        binding.hashSha1Value.visibility = View.VISIBLE
                        binding.hashSha1Value.text = result
                    }
                }
            }).start()
            
            // 加载 SHA256
            HashTask(fileItem, HashTask.HashType.SHA256, object : HashTask.CompletedCallback {
                override fun onHashCompleted(result: String) {
                    if (_binding != null) {
                        binding.hashSha256Progress.visibility = View.GONE
                        binding.hashSha256Value.visibility = View.VISIBLE
                        binding.hashSha256Value.text = result
                    }
                }
            }).start()
            
            // 加载 CRC32
            HashTask(fileItem, HashTask.HashType.CRC32, object : HashTask.CompletedCallback {
                override fun onHashCompleted(result: String) {
                    if (_binding != null) {
                        binding.hashCrc32Progress.visibility = View.GONE
                        binding.hashCrc32Value.visibility = View.VISIBLE
                        binding.hashCrc32Value.text = result
                    }
                }
            }).start()
            
            // 设置点击复制功能
            setupClickListeners()
        }
    }

    private fun setupClickListeners() {
        binding.hashMd5Area.setOnClickListener {
            val value = binding.hashMd5Value.text?.toString()
            if (!value.isNullOrEmpty()) copyToClipboard(value)
        }
        binding.hashSha1Area.setOnClickListener {
            val value = binding.hashSha1Value.text?.toString()
            if (!value.isNullOrEmpty()) copyToClipboard(value)
        }
        binding.hashSha256Area.setOnClickListener {
            val value = binding.hashSha256Value.text?.toString()
            if (!value.isNullOrEmpty()) copyToClipboard(value)
        }
        binding.hashCrc32Area.setOnClickListener {
            val value = binding.hashCrc32Value.text?.toString()
            if (!value.isNullOrEmpty()) copyToClipboard(value)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
