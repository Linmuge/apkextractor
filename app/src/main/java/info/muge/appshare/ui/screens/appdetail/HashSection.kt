package info.muge.appshare.ui.screens.appdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import info.muge.appshare.items.AppItem
import info.muge.appshare.tasks.HashTask
import info.muge.appshare.ui.theme.AppDimens
import kotlinx.coroutines.launch

/**
 * Hash内容 - 与原 HashFragment 完全一致
 */
@Composable
fun HashContent(appItem: AppItem) {
    val context = LocalContext.current
    val fileItem = appItem.getFileItem()

    var md5Hash by remember { mutableStateOf<String?>(null) }
    var sha1Hash by remember { mutableStateOf<String?>(null) }
    var sha256Hash by remember { mutableStateOf<String?>(null) }
    var crc32Hash by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fileItem) {
        launch { md5Hash = HashTask(fileItem, HashTask.HashType.MD5).execute() }
        launch { sha1Hash = HashTask(fileItem, HashTask.HashType.SHA1).execute() }
        launch { sha256Hash = HashTask(fileItem, HashTask.HashType.SHA256).execute() }
        launch { crc32Hash = HashTask(fileItem, HashTask.HashType.CRC32).execute() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.Space.lg)
    ) {
        DetailCard {
            HashItem(
                label = "MD5",
                value = md5Hash,
                onClick = { copyToClipboard(context, md5Hash) }
            )
            InfoDivider()
            HashItem(
                label = "SHA1",
                value = sha1Hash,
                onClick = { copyToClipboard(context, sha1Hash) }
            )
            InfoDivider()
            HashItem(
                label = "SHA256",
                value = sha256Hash,
                onClick = { copyToClipboard(context, sha256Hash) }
            )
            InfoDivider()
            HashItem(
                label = "CRC32",
                value = crc32Hash,
                onClick = { copyToClipboard(context, crc32Hash) }
            )
        }
    }
}

@Composable
private fun HashItem(
    label: String,
    value: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
