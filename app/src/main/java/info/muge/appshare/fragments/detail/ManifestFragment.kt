package info.muge.appshare.fragments.detail

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import info.muge.appshare.R
import info.muge.appshare.utils.AXMLPrinter
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Manifest Fragment
 * Displays decoded AndroidManifest.xml
 */
class ManifestFragment : BaseDetailFragment() {

    private lateinit var tvManifest: TextView
    private lateinit var loadingProgress: View
    private lateinit var contentCard: View
    private lateinit var errorView: View

    companion object {
        fun newInstance(packageName: String): ManifestFragment {
            return ManifestFragment().apply {
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
        return inflater.inflate(R.layout.fragment_manifest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvManifest = view.findViewById(R.id.tv_manifest)
        loadingProgress = view.findViewById(R.id.loading_progress)
        contentCard = view.findViewById(R.id.manifest_card)
        errorView = view.findViewById(R.id.tv_error)

        appItem?.let { item ->
            loadingProgress.visibility = View.VISIBLE
            contentCard.visibility = View.GONE
            errorView.visibility = View.GONE

            Thread {
                try {
                    val zipFile = ZipFile(item.getSourcePath())
                    val entry = zipFile.getEntry("AndroidManifest.xml")
                    var xml = ""
                    
                    if (entry != null) {
                        val inputStream: InputStream = zipFile.getInputStream(entry)
                        xml = AXMLPrinter.decode(inputStream)
                        inputStream.close()
                    }
                    zipFile.close()

                    Handler(Looper.getMainLooper()).post {
                        if (entry == null) {
                            errorView.visibility = View.VISIBLE
                            loadingProgress.visibility = View.GONE
                        } else {
                            tvManifest.text = xml
                            contentCard.visibility = View.VISIBLE
                            loadingProgress.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Handler(Looper.getMainLooper()).post {
                        errorView.visibility = View.VISIBLE
                        (errorView as TextView).text = e.toString()
                        loadingProgress.visibility = View.GONE
                    }
                }
            }.start()
        }
    }
}
