package info.muge.appshare.fragments.detail

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import info.muge.appshare.R
import info.muge.appshare.databinding.FragmentAppInfoBinding
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 应用信息 Fragment
 * 显示应用的基本信息：包名、版本、大小、安装时间等
 */
class AppInfoFragment : BaseDetailFragment() {

    private var _binding: FragmentAppInfoBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(packageName: String): AppInfoFragment {
            return AppInfoFragment().apply {
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
        _binding = FragmentAppInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        appItem?.let { item ->
            val packageInfo = item.getPackageInfo()
            
            // 设置各项信息
            binding.appDetailPackageName.text = item.getPackageName()
            binding.appDetailVersionName.text = item.getVersionName()
            binding.appDetailVersionCode.text = item.getVersionCode().toString()
            binding.appDetailSize.text = Formatter.formatFileSize(requireContext(), item.getSize())
            binding.appDetailInstallTime.text = SimpleDateFormat.getDateTimeInstance()
                .format(Date(packageInfo.firstInstallTime))
            binding.appDetailUpdateTime.text = SimpleDateFormat.getDateTimeInstance()
                .format(Date(packageInfo.lastUpdateTime))
            binding.appDetailMinimumApi.text = packageInfo.applicationInfo?.minSdkVersion?.toString() ?: "-"
            binding.appDetailTargetApi.text = packageInfo.applicationInfo?.targetSdkVersion?.toString() ?: "-"
            binding.appDetailIsSystemApp.text = getString(
                if ((packageInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM > 0)
                    R.string.word_yes else R.string.word_no
            )
            binding.appDetailPathValue.text = packageInfo.applicationInfo?.sourceDir ?: "-"
            binding.appDetailInstallerNameValue.text = item.getInstallSource()
            binding.appDetailUid.text = packageInfo.applicationInfo?.uid?.toString() ?: "-"
            binding.appDetailLauncherValue.text = item.getLaunchingClass() ?: "-"
            
            // New fields
            binding.appDetailDataDirValue.text = packageInfo.applicationInfo?.dataDir ?: "-"
            binding.appDetailNativeLibDirValue.text = packageInfo.applicationInfo?.nativeLibraryDir ?: "-"
            binding.appDetailProcessNameValue.text = packageInfo.applicationInfo?.processName ?: "-"
            binding.appDetailFlagsValue.text = getFlagsString(packageInfo.applicationInfo?.flags ?: 0)
            
            // 设置点击复制功能
            setupClickListeners()
        }
    }

    private fun getFlagsString(flags: Int): String {
        val flagList = mutableListOf<String>()
        if (flags and ApplicationInfo.FLAG_SYSTEM != 0) flagList.add("SYSTEM")
        if (flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) flagList.add("DEBUGGABLE")
        if (flags and ApplicationInfo.FLAG_HAS_CODE != 0) flagList.add("HAS_CODE")
        if (flags and ApplicationInfo.FLAG_PERSISTENT != 0) flagList.add("PERSISTENT")
        if (flags and ApplicationInfo.FLAG_FACTORY_TEST != 0) flagList.add("FACTORY_TEST")
        if (flags and ApplicationInfo.FLAG_ALLOW_TASK_REPARENTING != 0) flagList.add("ALLOW_TASK_REPARENTING")
        if (flags and ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA != 0) flagList.add("ALLOW_CLEAR_USER_DATA")
        if (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) flagList.add("UPDATED_SYSTEM_APP")
        if (flags and ApplicationInfo.FLAG_TEST_ONLY != 0) flagList.add("TEST_ONLY")
        if (flags and ApplicationInfo.FLAG_VM_SAFE_MODE != 0) flagList.add("VM_SAFE_MODE")
        if (flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0) flagList.add("ALLOW_BACKUP")
        if (flags and ApplicationInfo.FLAG_KILL_AFTER_RESTORE != 0) flagList.add("KILL_AFTER_RESTORE")
        if (flags and ApplicationInfo.FLAG_RESTORE_ANY_VERSION != 0) flagList.add("RESTORE_ANY_VERSION")
        if (flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0) flagList.add("EXTERNAL_STORAGE")
        if (flags and ApplicationInfo.FLAG_LARGE_HEAP != 0) flagList.add("LARGE_HEAP")
        if (flags and ApplicationInfo.FLAG_STOPPED != 0) flagList.add("STOPPED")
        if (flags and ApplicationInfo.FLAG_SUPPORTS_RTL != 0) flagList.add("SUPPORTS_RTL")
        if (flags and ApplicationInfo.FLAG_INSTALLED != 0) flagList.add("INSTALLED")
        if (flags and ApplicationInfo.FLAG_IS_DATA_ONLY != 0) flagList.add("IS_DATA_ONLY")
        
        return if (flagList.isEmpty()) "-" else flagList.joinToString(", ")
    }

    private fun setupClickListeners() {
        binding.appDetailPackageNameArea.setOnClickListener {
            copyToClipboard(binding.appDetailPackageName.text?.toString())
        }
        binding.appDetailVersionNameArea.setOnClickListener {
            copyToClipboard(binding.appDetailVersionName.text?.toString())
        }
        binding.appDetailVersionCodeArea.setOnClickListener {
            copyToClipboard(binding.appDetailVersionCode.text?.toString())
        }
        binding.appDetailSizeArea.setOnClickListener {
            copyToClipboard(binding.appDetailSize.text?.toString())
        }
        binding.appDetailInstallTimeArea.setOnClickListener {
            copyToClipboard(binding.appDetailInstallTime.text?.toString())
        }
        binding.appDetailUpdateTimeArea.setOnClickListener {
            copyToClipboard(binding.appDetailUpdateTime.text?.toString())
        }
        binding.appDetailMinimumApiArea.setOnClickListener {
            copyToClipboard(binding.appDetailMinimumApi.text?.toString())
        }
        binding.appDetailTargetApiArea.setOnClickListener {
            copyToClipboard(binding.appDetailTargetApi.text?.toString())
        }
        binding.appDetailIsSystemAppArea.setOnClickListener {
            copyToClipboard(binding.appDetailIsSystemApp.text?.toString())
        }
        binding.appDetailPathArea.setOnClickListener {
            copyToClipboard(binding.appDetailPathValue.text?.toString())
        }
        binding.appDetailInstallerNameArea.setOnClickListener {
            copyToClipboard(binding.appDetailInstallerNameValue.text?.toString())
        }
        binding.appDetailUidArea.setOnClickListener {
            copyToClipboard(binding.appDetailUid.text?.toString())
        }
        binding.appDetailLauncherArea.setOnClickListener {
            copyToClipboard(binding.appDetailLauncherValue.text?.toString())
        }
        
        // New click listeners
        binding.appDetailDataDirArea.setOnClickListener {
            copyToClipboard(binding.appDetailDataDirValue.text?.toString())
        }
        binding.appDetailNativeLibDirArea.setOnClickListener {
            copyToClipboard(binding.appDetailNativeLibDirValue.text?.toString())
        }
        binding.appDetailProcessNameArea.setOnClickListener {
            copyToClipboard(binding.appDetailProcessNameValue.text?.toString())
        }
        binding.appDetailFlagsArea.setOnClickListener {
            copyToClipboard(binding.appDetailFlagsValue.text?.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
