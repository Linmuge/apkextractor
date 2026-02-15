package info.muge.appshare.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.muge.appshare.R

/**
 * 隐私政策对话框 - 使用 BottomSheet 样式，与原实现保持一致
 *
 * @param onAgree 同意按钮点击回调
 * @param onDisagree 拒绝按钮点击回调
 */
@Composable
fun PrivacyDialog(
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    AppBottomSheet(
        title = "用户协议及隐私政策",
        onDismiss = {},
        dismissible = false,
        content = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            val annotatedString = buildPrivacyText(appName)

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { annotation ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            }
                    }
                )
            }
        }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDisagree,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "拒绝并退出",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAgree,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "同意",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

@Composable
private fun buildPrivacyText(appName: String) = buildAnnotatedString {
    append("感谢您使用$appName\n")
    append("我们非常重视您的个人信息及隐私保护,在您使用我们的产品前，请您认真阅读  ")

    // 服务协议链接
    pushStringAnnotation("URL", "https://link.appshare.muge.info/appkit/user.html")
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
        append("服务协议")
    }
    pop()

    append(" 与 ")

    // 隐私政策链接
    pushStringAnnotation("URL", "https://link.appshare.muge.info/appkit/privacy.html")
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
        append("隐私政策")
    }
    pop()

    append(
        " 的全部内容，同意后开始使用我们的产品。\n\n" +
        "更新日期：2026年1月4日\n" +
        "生效日期：2026年1月4日\n\n" +
        "郑重承诺：\n" +
        "1. 本应用不申请网络权限，无法连接互联网。\n" +
        "2. 您所有的操作、生成的APK/图片均只保存在您的手机本地。\n" +
        "3. 我们不收集、不上传任何用户数据。\n\n" +
        "权限说明：\n" +
        "1. android.permission.QUERY_ALL_PACKAGES\n" +
        "2. com.android.permission.GET_INSTALLED_APPS\n" +
        "以上权限仅用于获取本机已安装应用列表以展示及导出，数据绝不上传。"
    )
}
