package com.juhao.rollcall.ui.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.juhao.rollcall.data.Student
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.ConfirmActionButton
import com.juhao.rollcall.ui.components.HintText

/**
 * 添加 / 编辑一个学生：改名字、调顺序、删除。
 * 页面很短，用普通 Column + verticalScroll 即可，手表上没有弹窗，删除用两段式确认按钮。
 */
@Composable
fun StudentEditScreen(
    student: Student?,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var name by remember(student?.id) { mutableStateOf(student?.name ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (student == null) "添加学生" else "编辑学生",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )

        Text(
            text = "姓名",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )

        NameField(value = name, onValueChange = { name = it })

        if (student != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    text = "上移",
                    onClick = onMoveUp,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = "下移",
                    onClick = onMoveDown,
                    modifier = Modifier.weight(1f),
                )
            }
            HintText(
                text = "用「上移 / 下移」调整点名顺序",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AppButton(
            text = "保存",
            onClick = { onSave(name.trim()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank(),
        )

        if (student != null) {
            ConfirmActionButton(
                text = "删除该学生",
                confirmText = "确认删除?",
                onConfirm = onDelete,
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

/** 自绘单行输入框：Wear Material3 的 TextField 屏幕放不下，用 BasicTextField + Box 画。 */
@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        shape = shape,
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "输入姓名",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            lineHeight = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
                innerTextField()
            }
        },
    )
}
