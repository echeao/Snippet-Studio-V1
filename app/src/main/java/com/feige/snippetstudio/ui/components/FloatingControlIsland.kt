package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.ui.theme.*

@Composable
fun FloatingControlIsland(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    showSymbolBar: Boolean,
    onToggleSymbolBar: () -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = modifier
            .shadow(AppElevation.Md, RoundedCornerShape(R_XL), ambientColor = AppElevation.MdColor)
            .border(1.dp, tc.line, RoundedCornerShape(R_XL))
            .testTag("floating_control_island"),
        shape = RoundedCornerShape(R_XL),
        color = tc.surface.copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
        ) {
            Row(
                modifier = Modifier
                    .background(tc.surface2, RoundedCornerShape(R_MD))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (selectedTab == 0) tc.primary else Color.Transparent,
                    shape = RoundedCornerShape(R_SM),
                    modifier = Modifier.clickable { onSelectTab(0) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = "Code",
                            tint = if (selectedTab == 0) Color.White else tc.text2,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "代码",
                            style = CaptionStyle,
                            color = if (selectedTab == 0) Color.White else tc.text2
                        )
                    }
                }

                Surface(
                    color = if (selectedTab == 1) tc.primary else Color.Transparent,
                    shape = RoundedCornerShape(R_SM),
                    modifier = Modifier.clickable { onSelectTab(1) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Preview",
                            tint = if (selectedTab == 1) Color.White else tc.text2,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "预览",
                            style = CaptionStyle,
                            color = if (selectedTab == 1) Color.White else tc.text2
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                IconButton(
                    onClick = onToggleSymbolBar,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "Symbol Bar Toggle",
                        tint = if (showSymbolBar) tc.primary else tc.text2,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.height(18.dp),
                color = tc.line
            )

            IconButton(
                onClick = onExitFullscreen,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    tint = Danger,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
