package com.feige.snippetstudio.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToSubPage: (String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    // SAF Document Creator Launcher for JSON Backup
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupJson(context, uri) { success ->
                if (success) {
                    onShowSnackbar(context.getString(R.string.toast_exported))
                } else {
                    onShowSnackbar("Export failed")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = DisplayTitleStyle,
                        color = textPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) BgDark else BgLight
                )
            )
        },
        containerColor = if (isDark) BgDark else BgLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.S4),
            verticalArrangement = Arrangement.spacedBy(Spacing.S5)
        ) {
            // Group 1: Workspace Repo
            SettingsGroup(title = stringResource(R.string.set_repo)) {
                SettingsItem(
                    iconRes = R.drawable.ic_layers,
                    title = stringResource(R.string.set_repo_cur),
                    subTitle = settings.repoPath,
                    onClick = { onNavigateToSubPage("repo") }
                )
            }

            // Group 2: Sync & Version Control
            SettingsGroup(title = stringResource(R.string.set_sync)) {
                SettingsItem(
                    iconRes = R.drawable.ic_git,
                    title = stringResource(R.string.set_git),
                    subTitle = if (settings.gitConnected) stringResource(R.string.sub_git_connected) else stringResource(R.string.sub_git_disconnected),
                    onClick = { onNavigateToSubPage("git") }
                )
            }

            // Group 3: Content Organization
            SettingsGroup(title = stringResource(R.string.set_org)) {
                SettingsItem(
                    iconRes = R.drawable.ic_layers,
                    title = stringResource(R.string.set_cat),
                    onClick = { onNavigateToSubPage("cat") }
                )
                HorizontalDivider(color = if (isDark) LineDark else LineLight)
                SettingsItem(
                    iconRes = R.drawable.ic_tag,
                    title = stringResource(R.string.set_tags),
                    onClick = { onNavigateToSubPage("tags") }
                )
            }

            // Group 4: Data Maintenance
            SettingsGroup(title = stringResource(R.string.set_maintain)) {
                SettingsItem(
                    iconRes = R.drawable.ic_spark,
                    title = stringResource(R.string.set_backup),
                    onClick = {
                        createDocLauncher.launch("snippet-studio-backup.json")
                    }
                )
                HorizontalDivider(color = if (isDark) LineDark else LineLight)
                SettingsItem(
                    iconRes = R.drawable.ic_spark,
                    title = stringResource(R.string.set_trash),
                    onClick = { onNavigateToSubPage("trash") }
                )
            }

            // Group 5: Appearance & System
            SettingsGroup(title = stringResource(R.string.set_look)) {
                AppSwitch(
                    checked = (settings.theme == "dark" || (settings.theme == "system" && isDark)),
                    onCheckedChange = { viewModel.toggleDarkMode(it) },
                    label = stringResource(R.string.set_dark)
                )
                HorizontalDivider(color = if (isDark) LineDark else LineLight)
                val langLabel = when (settings.lang) {
                    "ja" -> stringResource(R.string.lang_ja)
                    "en" -> stringResource(R.string.lang_en)
                    else -> stringResource(R.string.lang_zh)
                }
                SettingsItem(
                    iconRes = R.drawable.ic_globe,
                    title = stringResource(R.string.set_lang),
                    subTitle = langLabel,
                    onClick = { onNavigateToSubPage("lang") }
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) LineDark else LineLight
    val titleColor = if (isDark) Text2Dark else Text2Light

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = CaptionStyle,
            color = titleColor,
            modifier = Modifier.padding(start = Spacing.S2, bottom = Spacing.S2)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD), ambientColor = AppElevation.SmColor)
                .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
            shape = RoundedCornerShape(R_MD),
            color = cardBg
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    iconRes: Int,
    title: String,
    subTitle: String? = null,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.S3))

            Column {
                Text(
                    text = title,
                    style = ListTitleStyle,
                    color = textPrimary
                )
                if (!subTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subTitle,
                        style = CaptionStyle,
                        color = textSecondary
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Navigate",
            tint = textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
