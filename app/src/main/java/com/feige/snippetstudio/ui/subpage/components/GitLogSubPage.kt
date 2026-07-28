package com.feige.snippetstudio.ui.subpage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.ui.subpage.vm.GitSubState
import com.feige.snippetstudio.ui.theme.*

/**
 * [GitLogSubPage] Git Commit 历史提交日志视图组件。
 *
 * 架构职责：
 * 1. 展现 Git 提交日志列表与时间线节点标尺。
 * 2. 呈现提交信息 (Commit Message)、Hash 简写短码、作者签名及提交时间戳。
 *
 * @param gitState Git 子页面专属 UI 状态
 * @param modifier 外部 Modifier
 */
@Composable
fun GitLogSubPage(
    gitState: GitSubState,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.S4),
        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
    ) {
        if (gitState.isGitLogLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(Spacing.S5),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = tc.primary)
            }
        } else if (gitState.gitLogError != null) {
            Text(
                text = gitState.gitLogError!!,
                style = BodyStyle,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(Spacing.S3)
            )
        } else if (gitState.gitLogCommits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(Spacing.S5),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无提交记录", style = BodyStyle, color = tc.text2)
            }
        } else {
            Text(
                text = "共 ${gitState.gitLogCommits.size} 条提交记录",
                style = CaptionStyle,
                color = tc.text2,
                modifier = Modifier.padding(horizontal = Spacing.S2)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.S2),
                modifier = Modifier.fillMaxSize()
            ) {
                items(gitState.gitLogCommits, key = { it.commitId }) { commit ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(R_SM),
                        color = tc.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, tc.line)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.S3),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.S3)
                        ) {
                            // 左侧时间线圆点
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(tc.primary, CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = commit.message,
                                    style = ListTitleStyle,
                                    color = tc.text,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                                ) {
                                    Text(
                                        text = commit.shortId,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = tc.primary
                                    )
                                    Text(
                                        text = commit.author,
                                        fontSize = 11.sp,
                                        color = tc.text2
                                    )
                                }
                                Text(
                                    text = com.feige.snippetstudio.util.TimeUtil.formatFullDateTime(commit.timestamp),
                                    fontSize = 10.5.sp,
                                    color = tc.text2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
