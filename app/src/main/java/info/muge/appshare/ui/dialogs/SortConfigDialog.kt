package info.muge.appshare.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.muge.appshare.Constants
import info.muge.appshare.R

/**
 * 排序配置对话框 - 与原 AppItemSortConfigDialog 完全一致
 */
@Composable
fun SortConfigDialog(
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit,
    currentSort: Int = 0
) {
    var selectedOption by remember { mutableIntStateOf(currentSort) }

    // 与原 dialog_sort.xml 完全一致的选项
    val options = listOf(
        stringResource(R.string.dialog_sort_default) to 0,
        stringResource(R.string.dialog_sort_ascending_name) to 1,
        stringResource(R.string.dialog_sort_descending_name) to 2,
        stringResource(R.string.dialog_sort_ascending_package_name) to 3,
        stringResource(R.string.dialog_sort_descending_package_name) to 4,
        stringResource(R.string.dialog_sort_ascending_size) to 5,
        stringResource(R.string.dialog_sort_descending_size) to 6,
        stringResource(R.string.dialog_sort_ascending_date) to 7,
        stringResource(R.string.dialog_sort_descending_date) to 8,
        stringResource(R.string.dialog_sort_ascending_install_date) to 9,
        stringResource(R.string.dialog_sort_descending_install_date) to 10
    )

    AppBottomSheet(
        title = stringResource(R.string.dialog_sort_appitem_title),
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedOption == value),
                                onClick = { selectedOption = value }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == value),
                            onClick = null
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = { onOptionSelected(selectedOption) },
                onDismiss = onDismiss,
                confirmText = stringResource(android.R.string.ok),
                dismissText = stringResource(R.string.dialog_button_cancel)
            )
        }
    )
}
