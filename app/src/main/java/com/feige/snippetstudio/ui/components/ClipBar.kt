package com.feige.snippetstudio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*
import com.feige.snippetstudio.util.DetectedClip

/**
 * [ClipBar] 剪贴板检测浮动快捷保存横幅组件。
 *
 * 当检测到系统剪贴板符合代码/Prompt 特征时弹出，支持快速另存为片段或忽略关闭。
 *
 * @param clip 检出的剪贴板内容对象 (未检测到或忽略时为 null)
 * @param onSave 保存点击回调
 * @param onDismiss 忽略/关闭点击回调
 * @param modifier 外部修饰符
 */
@Composable
fun ClipBar(
    clip: DetectedClip?,
    onSave: (DetectedClip) -> Unit,
    onDismiss: (DetectedClip) -> Unit,
    modifier: Modifier = Modifier
) {

    val tc = LocalThemeColors.current

    AnimatedVisibility(
        visible = (clip != null),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        if (clip != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.S4, vertical = Spacing.S2)
                    .border(1.dp, tc.line, RoundedCornerShape(R_MD))
                    .testTag("clip_bar"),
                shape = RoundedCornerShape(R_MD),
                color = tc.primarySoft
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.S3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clipboard),
                        contentDescription = "Clipboard Code",
                        tint = tc.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(Spacing.S3))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_clip_title),
                            style = ListTitleStyle,
                            color = tc.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = clip.previewText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = tc.text2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.S2))

                    TextButton(
                        onClick = { onDismiss(clip) },
                        modifier = Modifier.testTag("clip_ignore_btn")
                    ) {
                        Text(stringResource(R.string.home_clip_ignore), color = tc.text2, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onSave(clip) },
                        shape = AppShapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = tc.primary),
                        contentPadding = PaddingValues(horizontal = Spacing.S3, vertical = Spacing.S1),
                        modifier = Modifier.testTag("clip_save_btn")
                    ) {
                        Text(stringResource(R.string.home_clip_save), fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
