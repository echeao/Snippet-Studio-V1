package com.feige.snippetstudio.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

/**
 * [HomeTopBar] 首页顶部 App 标题栏组件。
 *
 * 包含应用程序主标题、版本/定位标签徽章以及右上角快捷同步刷新按钮。
 *
 * @param onRefreshClick 点击右上角同步刷新按钮时的回调闭包
 * @param modifier 外部修饰符 Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = DisplayTitleStyle,
                    color = tc.text
                )
                Spacer(modifier = Modifier.width(Spacing.S2))
                Surface(
                    color = tc.primarySoft,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = BadgeStyle.copy(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = tc.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier.testTag("home_refresh_btn")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "Refresh",
                    tint = tc.text2
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = tc.bg
        ),
        modifier = modifier
    )
}
