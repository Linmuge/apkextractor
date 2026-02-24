package info.muge.appshare.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.muge.appshare.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface GlobalDialogState {
    data class Confirm(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit = {}
    ) : GlobalDialogState

    data class DataObbSelection(
        val title: String,
        val dataLabel: String,
        val obbLabel: String,
        val onConfirm: (Boolean, Boolean) -> Unit,
        val onDismiss: () -> Unit = {}
    ) : GlobalDialogState

    data class Progress(
        val title: String,
        val indeterminate: Boolean,
        val cancelText: String,
        val onCancel: () -> Unit,
        val progress: Int = 0,
        val total: Int = 0,
        val message: String = ""
    ) : GlobalDialogState
}

object GlobalDialogManager {
    private val _state = MutableStateFlow<GlobalDialogState?>(null)
    val state: StateFlow<GlobalDialogState?> = _state.asStateFlow()

    fun showConfirm(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        _state.value = GlobalDialogState.Confirm(title, message, onConfirm, onDismiss)
    }

    fun showDataObbSelection(
        title: String,
        dataLabel: String,
        obbLabel: String,
        onConfirm: (Boolean, Boolean) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        _state.value = GlobalDialogState.DataObbSelection(
            title = title,
            dataLabel = dataLabel,
            obbLabel = obbLabel,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    fun showProgress(
        title: String,
        indeterminate: Boolean,
        cancelText: String,
        onCancel: () -> Unit
    ) {
        _state.value = GlobalDialogState.Progress(
            title = title,
            indeterminate = indeterminate,
            cancelText = cancelText,
            onCancel = onCancel
        )
    }

    fun updateProgress(progress: Int, total: Int, message: String = "") {
        val current = _state.value as? GlobalDialogState.Progress ?: return
        _state.value = current.copy(progress = progress, total = total, message = message)
    }

    fun dismiss() {
        _state.value = null
    }
}

@Composable
fun GlobalDialogHost() {
    val state by GlobalDialogManager.state.collectAsStateWithLifecycle()

    when (val current = state) {
        null -> Unit

        is GlobalDialogState.Confirm -> {
            ConfirmDialog(
                title = current.title,
                message = current.message,
                onConfirm = {
                    GlobalDialogManager.dismiss()
                    current.onConfirm()
                },
                onDismiss = {
                    GlobalDialogManager.dismiss()
                    current.onDismiss()
                }
            )
        }

        is GlobalDialogState.DataObbSelection -> {
            var exportData by remember(current) { mutableStateOf(false) }
            var exportObb by remember(current) { mutableStateOf(false) }

            AppBottomSheet(
                title = current.title,
                onDismiss = {
                    GlobalDialogManager.dismiss()
                    current.onDismiss()
                },
                content = {
                    Column {
                        RowCheckbox(
                            label = current.dataLabel,
                            checked = exportData,
                            onCheckedChange = { exportData = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RowCheckbox(
                            label = current.obbLabel,
                            checked = exportObb,
                            onCheckedChange = { exportObb = it }
                        )
                    }
                },
                actions = {
                    AppBottomSheetDualActions(
                        onConfirm = {
                            GlobalDialogManager.dismiss()
                            current.onConfirm(exportData, exportObb)
                        },
                        onDismiss = {
                            GlobalDialogManager.dismiss()
                            current.onDismiss()
                        },
                        confirmText = stringResource(R.string.dialog_button_confirm),
                        dismissText = stringResource(R.string.dialog_button_cancel)
                    )
                }
            )
        }

        is GlobalDialogState.Progress -> {
            AppBottomSheet(
                title = current.title,
                onDismiss = {},
                dismissible = false,
                content = {
                    Column {
                        if (current.indeterminate) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            val fraction = if (current.total > 0) {
                                current.progress.toFloat() / current.total.toFloat()
                            } else {
                                0f
                            }
                            LinearProgressIndicator(
                                progress = { fraction.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${current.progress} / ${current.total}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (current.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = current.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    AppBottomSheetActions(
                        onConfirm = current.onCancel,
                        confirmText = current.cancelText
                    )
                }
            )
        }
    }
}

@Composable
private fun RowCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
