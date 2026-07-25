package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.model.PromptVariable
import com.feige.snippetstudio.ui.theme.*

/**
 * [VariableFillPanel] Prompt 动态变量填充面板 (ModalBottomSheet)。
 *
 * 当编辑器检测到 Prompt 文本中存在 `{{变量}}` 占位符时，
 * 用户可通过此面板快捷填写各变量的值，并一键应用替换。
 *
 * @param show 显隐控制
 * @param variables 检测到的变量列表
 * @param variableValues 当前各变量的填充值
 * @param onValueChange 变量值变更回调
 * @param onApply 应用填充回调
 * @param onDismiss 关闭面板回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariableFillPanel(
    show: Boolean,
    variables: List<PromptVariable>,
    variableValues: Map<String, String>,
    onValueChange: (name: String, value: String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    val tc = LocalThemeColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tc.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.S5, vertical = Spacing.S3)
                .padding(bottom = Spacing.S6),
            verticalArrangement = Arrangement.spacedBy(Spacing.S4)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.var_panel_title),
                    style = SectionTitleStyle,
                    color = tc.text
                )
                // 变量计数徽章
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tc.primarySoft
                ) {
                    Text(
                        text = "${variables.size}",
                        style = CaptionStyle,
                        color = tc.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (variables.isEmpty()) {
                Text(
                    text = stringResource(R.string.var_panel_empty),
                    style = CaptionStyle,
                    color = tc.text2,
                    modifier = Modifier.padding(vertical = Spacing.S4)
                )
            } else {
                // 变量输入列表
                variables.forEach { variable ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S1)
                    ) {
                        // 变量名标签
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                        ) {
                            Text(
                                text = "{{${variable.name}}}",
                                style = CaptionStyle,
                                fontSize = 12.sp,
                                color = tc.primary,
                                fontWeight = FontWeight.W600
                            )
                            if (variable.occurrences.size > 1) {
                                Text(
                                    text = "×${variable.occurrences.size}",
                                    style = CaptionStyle,
                                    fontSize = 11.sp,
                                    color = tc.text3
                                )
                            }
                        }

                        // 输入框
                        OutlinedTextField(
                            value = variableValues[variable.name] ?: "",
                            onValueChange = { onValueChange(variable.name, it) },
                            placeholder = {
                                Text(
                                    text = variable.defaultValue.ifBlank {
                                        stringResource(R.string.var_panel_input_hint)
                                    },
                                    style = CaptionStyle
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    }
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        style = ListTitleStyle,
                        color = tc.text2
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.S3))
                Button(
                    onClick = onApply,
                    enabled = variables.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = tc.primary)
                ) {
                    Text(
                        text = stringResource(R.string.var_panel_apply),
                        style = ListTitleStyle,
                        fontWeight = FontWeight.W600
                    )
                }
            }
        }
    }
}
