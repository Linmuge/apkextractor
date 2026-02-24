package info.muge.appshare.ui.screens.appdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.theme.AppDimens
import info.muge.appshare.utils.AXMLPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

/**
 * Manifest内容 - 与原 ManifestFragment 完全一致
 */
@Composable
fun ManifestContent(appItem: AppItem) {
    var manifestContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appItem) {
        try {
            val result = withContext(Dispatchers.IO) {
                val zipFile = ZipFile(appItem.getSourcePath())
                val entry = zipFile.getEntry("AndroidManifest.xml")
                val content = if (entry != null) {
                    val inputStream = zipFile.getInputStream(entry)
                    val decoded = AXMLPrinter.decode(inputStream)
                    inputStream.close()
                    decoded
                } else {
                    null
                }
                zipFile.close()
                content
            }
            if (result != null) {
                manifestContent = result
            } else {
                error = "未找到 AndroidManifest.xml"
            }
        } catch (e: Exception) {
            error = e.toString()
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (error != null) {
            Text(
                text = error ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(AppDimens.Space.lg)
            )
        } else if (manifestContent != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppDimens.Space.lg)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimens.Radius.xl),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Text(
                        text = manifestContent ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.Space.lg)
                    )
                }
            }
        }
    }
}
