package com.feige.snippetstudio.ui.subpage

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubPageScreen(
    viewModel: SubPageViewModel,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light
    val cardBg = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) LineDark else LineLight

    var pendingPurgeId by remember { mutableStateOf<String?>(null) }

    // SAF Document Tree Launcher
    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val pathName = uri.lastPathSegment ?: uri.toString()
                viewModel.updateRepoPath(context, pathName, uri.toString())
                onShowSnackbar(context.getString(R.string.toast_saved))
            } catch (e: Exception) {
                val pathName = uri.lastPathSegment ?: uri.toString()
                viewModel.updateRepoPath(context, pathName, uri.toString())
                onShowSnackbar("Updated path: ${uri.lastPathSegment}")
            }
        }
    }

    val pageTitle = when (uiState.key) {
        "repo" -> stringResource(R.string.set_repo)
        "git" -> stringResource(R.string.set_git)
        "cat" -> stringResource(R.string.set_cat)
        "tags" -> stringResource(R.string.set_tags)
        "trash" -> stringResource(R.string.set_trash)
        "lang" -> stringResource(R.string.set_lang)
        else -> stringResource(R.string.settings_title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("subpage_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = pageTitle,
                        style = SectionTitleStyle,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.key) {
                "repo" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S4)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD))
                                .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                            shape = RoundedCornerShape(R_MD),
                            color = cardBg
                        ) {
                            Column(modifier = Modifier.padding(Spacing.S4)) {
                                Text(stringResource(R.string.set_repo_cur), style = CaptionStyle, color = textSecondary)
                                Spacer(modifier = Modifier.height(Spacing.S2))
                                Text(
                                    text = uiState.settings.repoPath,
                                    style = ListTitleStyle,
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(Spacing.S4))
                                Button(
                                    onClick = { openTreeLauncher.launch(null) },
                                    shape = AppShapes.small,
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Text(stringResource(R.string.sub_repo_change))
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.sub_repo_hint),
                            style = BodyStyle,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = Spacing.S2)
                        )
                    }
                }

                "git" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S4)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(AppElevation.Sm, RoundedCornerShape(R_MD))
                                .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                            shape = RoundedCornerShape(R_MD),
                            color = cardBg
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.S4),
                                verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                            ) {
                                OutlinedTextField(
                                    value = uiState.gitUrlInput,
                                    onValueChange = { viewModel.onGitUrlChange(it) },
                                    label = { Text(stringResource(R.string.sub_git_url)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = uiState.gitBranchInput,
                                    onValueChange = { viewModel.onGitBranchChange(it) },
                                    label = { Text(stringResource(R.string.sub_git_branch)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = uiState.gitPatInput,
                                    onValueChange = { viewModel.onGitPatChange(it) },
                                    label = { Text(stringResource(R.string.sub_git_pat)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(Spacing.S2))

                                Button(
                                    onClick = {
                                        viewModel.testGitConnection { connected ->
                                            onShowSnackbar(
                                                if (connected) context.getString(R.string.sub_git_connected) else context.getString(R.string.sub_git_disconnected)
                                            )
                                        }
                                    },
                                    shape = AppShapes.small,
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.sub_git_test))
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.sub_git_hint),
                            style = BodyStyle,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = Spacing.S2)
                        )
                    }
                }

                "cat" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        listOf("HTML", "JavaScript", "Markdown", "Prompt").forEach { cat ->
                            val count = uiState.categoryCounts[cat] ?: 0
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                                shape = RoundedCornerShape(R_MD),
                                color = cardBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.S4),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = cat, style = ListTitleStyle, color = textPrimary)
                                    Surface(
                                        color = PrimarySoft,
                                        shape = RoundedCornerShape(R_SM)
                                    ) {
                                        Text(
                                            text = "$count 项",
                                            style = BadgeStyle,
                                            color = Primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "tags" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4)
                    ) {
                        if (uiState.tags.isEmpty()) {
                            EmptyState(
                                title = "尚无自定义标签",
                                desc = "在片段中添加标签后在此集中展示"
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.S2)
                            ) {
                                uiState.tags.forEach { tag ->
                                    Surface(
                                        color = C_TagBg,
                                        shape = RoundedCornerShape(R_SM)
                                    ) {
                                        Text(
                                            text = "# $tag",
                                            style = ListTitleStyle,
                                            color = C_Tag,
                                            modifier = Modifier.padding(horizontal = Spacing.S3, vertical = Spacing.S2)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "trash" -> {
                    if (uiState.trashedSnippets.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.empty_trash_title),
                            desc = stringResource(R.string.confirm_trash_desc)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.S4),
                            verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                        ) {
                            items(
                                items = uiState.trashedSnippets,
                                key = { it.id }
                            ) { snippet ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, borderColor, RoundedCornerShape(R_MD)),
                                    shape = RoundedCornerShape(R_MD),
                                    color = cardBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(Spacing.S4),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = snippet.displayTitle, style = ListTitleStyle, color = textPrimary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = snippet.type.displayName, style = CaptionStyle, color = textSecondary)
                                        }

                                        Row {
                                            TextButton(
                                                onClick = {
                                                    viewModel.restoreSnippet(snippet.id)
                                                    onShowSnackbar(context.getString(R.string.toast_restored))
                                                }
                                            ) {
                                                Icon(imageVector = Icons.Filled.Restore, contentDescription = "Restore", tint = Success)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(stringResource(R.string.act_restore), color = Success)
                                            }

                                            TextButton(
                                                onClick = { pendingPurgeId = snippet.id }
                                            ) {
                                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Purge", tint = Danger)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(stringResource(R.string.act_purge), color = Danger)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "lang" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.S4),
                        verticalArrangement = Arrangement.spacedBy(Spacing.S3)
                    ) {
                        listOf(
                            "zh" to stringResource(R.string.lang_zh),
                            "ja" to stringResource(R.string.lang_ja),
                            "en" to stringResource(R.string.lang_en)
                        ).forEach { (code, label) ->
                            val isSelected = uiState.settings.lang == code
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isSelected) Primary else borderColor, RoundedCornerShape(R_MD))
                                    .clickable {
                                        viewModel.setLanguage(context, code)
                                        onShowSnackbar(context.getString(R.string.toast_saved))
                                    },
                                shape = RoundedCornerShape(R_MD),
                                color = if (isSelected) PrimarySoft else cardBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.S4),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = ListTitleStyle,
                                        color = if (isSelected) Primary else textPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Purge confirmation dialog
        ConfirmDialog(
            show = (pendingPurgeId != null),
            title = stringResource(R.string.confirm_purge_title),
            desc = stringResource(R.string.confirm_purge_desc),
            onConfirm = {
                pendingPurgeId?.let { id ->
                    viewModel.purgeSnippet(id)
                    onShowSnackbar("Deleted forever")
                }
            },
            onDismiss = { pendingPurgeId = null },
            isDanger = true
        )
    }
}
