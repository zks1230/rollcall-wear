package com.juhao.rollcall.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.juhao.rollcall.data.NameListParser
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.ConfirmActionButton
import com.juhao.rollcall.ui.components.HintText

/**
 * 批量导入：把微信 / Excel / 记事本里的名字整段粘进来，实时解析预览，再追加或整体替换。
 * 解析规则交给 data/NameListParser，这里只负责画。
 */
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImport: (names: List<String>, replace: Boolean) -> Unit,
) {
    var raw by remember { mutableStateOf("") }
    val parsed = remember(raw) { NameListParser.parse(raw) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "批量导入",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )

        PasteField(value = raw, onValueChange = { raw = it })

        if (parsed.isEmpty()) {
            HintText(
                text = "还没有识别到名字，粘贴后自动预览",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = buildString {
                    append("识别到 ")
                    append(parsed.size)
                    append(" 人：")
                    append(parsed.take(8).joinToString("、"))
                    if (parsed.size > 8) append("…")
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AppButton(
            text = "追加到名单",
            onClick = { onImport(parsed, false) },
            modifier = Modifier.fillMaxWidth(),
            enabled = parsed.isNotEmpty(),
        )

        if (parsed.isNotEmpty()) {
            ConfirmActionButton(
                text = "替换整个名单",
                confirmText = "确认替换?",
                onConfirm = { onImport(parsed, true) },
                modifier = Modifier.fillMaxWidth(),
            )
            HintText(
                text = "替换会按名字沿用旧 id，历史记录不丢",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AppButton(
            text = "返回",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            filled = false,
        )
    }
}

/** 多行粘贴框：自绘背景，最小高度 88dp，占位提示分隔符。 */
@Composable
private fun PasteField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.labelLarge.copy(
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        shape = shape,
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "每行一个名字，或用逗号、顿号、空格分隔",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                innerTextField()
            }
        },
    )
}
