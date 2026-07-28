package com.feige.snippetstudio.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.EmptyState
import com.feige.snippetstudio.ui.theme.*

/**
 * [RecentHeader] 最近代码片段分类标题栏组件。
 *
 * 展示“最近修改”模块名称与右上角跳转至完整文件仓库的链接。
 *
 * @param totalActiveCount 全局活动代码片段总数
 * @param onNavigateToFiles 跳转至全量文件列表页面的回调闭包
 * @param modifier 外部修饰符 Modifier
 */
@Composable
fun RecentHeader(
    totalActiveCount: Int,
    onNavigateToFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.S4, vertical = Spacing.S3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_code),
                contentDescription = null,
                tint = tc.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.S2))
            Text(
                text = stringResource(R.string.home_recent),
                style = SectionTitleStyle,
                color = tc.text
            )
        }

        Text(
            text = "${stringResource(R.string.home_view_all)} ($totalActiveCount)",
            style = ListTitleStyle,
            color = tc.primary,
            modifier = Modifier
                .clickable { onNavigateToFiles() }
                .testTag("view_all_link")
        )
    }
}

/**
 * [HomeEmptyState] 首页搜索或空片段时的占位组件。
 *
 * 当用户在搜索框输入关键词无匹配项时，提供一键“清除搜索词”的交互响应操作。
 *
 * @param searchQuery 当前搜索关键字
 * @param onClearSearch 清除搜索词的回调
 * @param onNewSnippet 点击新建片段的回调
 */
@Composable
fun HomeEmptyState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    onNewSnippet: () -> Unit
) {
    if (searchQuery.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            EmptyState(
                title = stringResource(R.string.empty_filter_title),
                desc = stringResource(R.string.empty_filter_desc)
            )
            Spacer(modifier = Modifier.height(Spacing.S2))
            OutlinedButton(onClick = onClearSearch) {
                Text(text = "清空搜索关键字")
            }
        }
    } else {
        EmptyState(
            title = stringResource(R.string.empty_none_title),
            desc = stringResource(R.string.empty_none_desc),
            actionLabel = stringResource(R.string.sheet_new_title),
            onAction = onNewSnippet
        )
    }
}
