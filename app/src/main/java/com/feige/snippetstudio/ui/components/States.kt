package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

@Composable
fun EmptyState(
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconVector: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light
    val iconBg = if (isDark) Surface2Dark else Surface2Light

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.S6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(iconBg, RoundedCornerShape(R_XL)),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconRes != null -> {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Empty State Icon",
                        tint = Primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                iconVector != null -> {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = "Empty State Icon",
                        tint = Primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "Empty State Icon",
                        tint = Primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.S4))

        Text(
            text = title,
            style = SectionTitleStyle,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.S2))

        Text(
            text = desc,
            style = BodyStyle,
            color = textSecondary,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(Spacing.S5))
            Button(
                onClick = onAction,
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.testTag("empty_state_action_btn")
            ) {
                Text(text = actionLabel, style = ListTitleStyle)
            }
        }
    }
}

@Composable
fun LoadingState(
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val boxBg = if (isDark) Surface2Dark else Surface2Light

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.S4),
        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
    ) {
        repeat(3) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(R_MD),
                color = boxBg
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.S4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isDark) LineDark else LineLight, RoundedCornerShape(R_SM))
                    )
                    Spacer(modifier = Modifier.width(Spacing.S3))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(16.dp)
                                .background(if (isDark) LineDark else LineLight, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(12.dp)
                                .background(if (isDark) LineDark else LineLight, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorState(
    title: String = stringResource(R.string.error_title),
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.S6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Error",
            tint = Danger,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(Spacing.S3))

        Text(
            text = title,
            style = SectionTitleStyle,
            color = if (isDark) TextDark else TextLight
        )

        Spacer(modifier = Modifier.height(Spacing.S4))

        Button(
            onClick = onRetry,
            shape = AppShapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(stringResource(R.string.common_retry))
        }
    }
}
