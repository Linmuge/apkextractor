package info.muge.appshare.ui.screens.appdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.theme.AppDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

/**
 * Native库数据
 */
data class SoLibItem(
    val name: String,
    val arch: String,
    val fullPath: String
)

/**
 * Native库内容 - 与原 SoLibFragment 完全一致
 */
@Composable
fun SoLibContent(appItem: AppItem) {
    val context = LocalContext.current
    val libs = remember { mutableStateListOf<SoLibItem>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(appItem) {
        isLoading = true
        libs.clear()

        val items = withContext(Dispatchers.IO) {
            val result = mutableListOf<SoLibItem>()
            try {
                val sourcePath = appItem.getSourcePath()
                if (sourcePath.isNotEmpty()) {
                    val zipFile = ZipFile(sourcePath)
                    val entries = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (name.startsWith("lib/") && name.endsWith(".so")) {
                            val parts = name.split("/")
                            if (parts.size >= 3) {
                                result.add(SoLibItem(parts.last(), parts[1], name))
                            }
                        }
                    }
                    zipFile.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            result.sortWith(compareBy({ it.arch }, { it.name }))
            result
        }

        libs.addAll(items)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (libs.isEmpty()) {
            Text(
                text = "暂无Native库",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(libs) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { copyToClipboard(context, item.name) },
                        shape = RoundedCornerShape(AppDimens.Radius.md),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.Space.lg)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.arch,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
