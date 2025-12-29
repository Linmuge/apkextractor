package info.muge.appshare.fragments.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import info.muge.appshare.databinding.FragmentSignatureBinding
import info.muge.appshare.tasks.GetSignatureInfoTask

/**
 * 签名信息 Fragment
 * 显示 APK 签名的证书信息
 */
class SignatureFragment : BaseDetailFragment() {

    private var _binding: FragmentSignatureBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(packageName: String): SignatureFragment {
            return SignatureFragment().apply {
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
        _binding = FragmentSignatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        appItem?.let { item ->
            // 显示加载指示器
            binding.loadingProgress.visibility = View.VISIBLE
            binding.signatureCard.visibility = View.GONE
            
            // 加载签名信息
            GetSignatureInfoTask(
                requireActivity(),
                item.getPackageInfo(),
                binding.signatureView,
                object : GetSignatureInfoTask.CompletedCallback {
                    override fun onCompleted() {
                        if (_binding != null) {
                            binding.loadingProgress.visibility = View.GONE
                            binding.signatureCard.visibility = View.VISIBLE
                        }
                    }
                }
            ).start()
        } ?: run {
            binding.loadingProgress.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
