package info.muge.appshare.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.muge.appshare.R

/**
 * 进度对话框
 */
@Composable
fun ProgressDialog(
    title: String,
    progress: Int,
    total: Int,
    showKeepScreenOn: Boolean = true,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        title = title,
        onDismiss = onDismiss,
        dismissible = false,
        content = {
            Column {
                LinearProgressIndicator(
                    progress = { if (total > 0) progress.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$progress / $total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    val percentage = if (total > 0) (progress.toFloat() / total * 100).toInt() else 0
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            AppBottomSheetActions(
                onConfirm = onDismiss,
                confirmText = stringResource(R.string.word_stop)
            )
        }
    )
}

/**
 * 导出进度对话框
 */
@Composable
fun ExportingDialog(
    current: Int,
    total: Int,
    currentAppName: String,
    onDismiss: () -> Unit
) {
    ProgressDialog(
        title = "正在导出...",
        progress = current,
        total = total,
        onDismiss = onDismiss
    )
}

/**
 * 导入进度对话框
 */
@Composable
fun ImportingDialog(
    current: Int,
    total: Int,
    onDismiss: () -> Unit
) {
    ProgressDialog(
        title = "正在导入...",
        progress = current,
        total = total,
        onDismiss = onDismiss
    )
}

/**
 * 加载列表对话框
 */
@Composable
fun LoadingListDialog(
    title: String,
    progress: Int,
    total: Int,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        title = title,
        onDismiss = onDismiss,
        dismissible = false,
        content = {
            Column(
                modifier = Modifier.heightIn(min = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$progress / $total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            AppBottomSheetActions(
                onConfirm = onDismiss,
                confirmText = stringResource(R.string.dialog_button_cancel)
            )
        }
    )
}

/**
 * 确认对话框
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        title = title,
        onDismiss = onDismiss,
        content = {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmText = stringResource(android.R.string.ok),
                dismissText = stringResource(android.R.string.cancel)
            )
        }
    )
}

/**
 * 文件传输对话框
 */
@Composable
fun FileTransferringDialog(
    progress: Int,
    total: Int,
    fileName: String,
    onDismiss: () -> Unit
) {
    ProgressDialog(
        title = "正在传输 $fileName",
        progress = progress,
        total = total,
        onDismiss = onDismiss
    )
}
