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
            
            // 设置点击复制功能
            setupClickListeners()
        }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
