package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val tc = LocalThemeColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(AppElevation.Sm, RoundedCornerShape(R_LG), ambientColor = AppElevation.SmColor)
            .border(
                1.dp,
                if (value.isNotEmpty()) tc.primary.copy(alpha = 0.5f) else tc.line.copy(alpha = if (tc.isDark) 0.18f else 0.08f),
                RoundedCornerShape(R_LG)
            ),
        shape = RoundedCornerShape(R_LG),
        color = tc.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.S3 + 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Search",
                tint = if (value.isNotEmpty()) tc.primary else tc.text3,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.S3))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(fontSize = 14.5.sp, color = tc.text3.copy(alpha = 0.7f))
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontSize = 14.5.sp,
                        color = tc.text
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(tc.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )
            }

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("search_clear")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Clear search",
                        tint = tc.text2,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
