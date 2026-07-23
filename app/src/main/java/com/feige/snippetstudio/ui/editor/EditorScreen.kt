package com.feige.snippetstudio.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.components.*
import com.feige.snippetstudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) TextDark else TextLight
    val textSecondary = if (isDark) Text2Dark else Text2Light

    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleBack = {
        if (uiState.saveState == SaveState.UNSAVED) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) BgDark else BgLight),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val saveBadgeText = when (uiState.saveState) {
        SaveState.SAVED -> stringResource(R.string.state_saved)
        SaveState.SAVING -> stringResource(R.string.state_saving)
        SaveState.UNSAVED -> stringResource(R.string.state_unsaved)
    }

    val saveBadgeBg = when (uiState.saveState) {
        SaveState.SAVED -> SuccessSoft
        SaveState.SAVING -> WarningSoft
        SaveState.UNSAVED -> DangerSoft
    }

    val saveBadgeFg = when (uiState.saveState) {
        SaveState.SAVED -> Success
        SaveState.SAVING -> Warning
        SaveState.UNSAVED -> Danger
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { handleBack() },
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = uiState.title,
                            onValueChange = { viewModel.onTitleChange(it) },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W800,
                                color = textPrimary
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(Primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_title_input"),
                            decorationBox = { innerTextField ->
                                if (uiState.title.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.editor_rename_hint),
                                        style = TextStyle(fontSize = 16.sp, color = textSecondary)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(Spacing.S2))

                        // Save status badge
                        Surface(
                            color = saveBadgeBg,
                            shape = RoundedCornerShape(R_SM)
                        ) {
                            Text(
                                text = saveBadgeText,
                                style = BadgeStyle,
                                color = saveBadgeFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu placeholder */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) BgDark else BgLight
                )
            )
        },
        bottomBar = {
            // Status bar at bottom
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .border(1.dp, if (isDark) LineDark else LineLight),
                color = if (isDark) Surface2Dark else Surface2Light
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.S3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.currentLineIndex + 1}:${uiState.currentColumnIndex + 1}  ·  ${uiState.lineCount} 行  ·  ${uiState.charCount} 字符",
                        style = CaptionStyle,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        color = PrimarySoft,
                        shape = RoundedCornerShape(R_SM)
                    ) {
                        Text(
                            text = uiState.type.displayName,
                            style = BadgeStyle,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        containerColor = if (isDark) BgDark else BgLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Editor Toolbar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.S3, vertical = Spacing.S2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Control [Code | Preview]
                SegmentedControl(
                    options = listOf(stringResource(R.string.editor_code), stringResource(R.string.editor_preview)),
                    selectedIndex = uiState.selectedTab,
                    onSelect = { viewModel.selectTab(it) }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Font Size - / +
                    TextButton(
                        onClick = { viewModel.adjustFontSize(-1f) },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("A−", style = CaptionStyle, color = textSecondary)
                    }
                    TextButton(
                        onClick = { viewModel.adjustFontSize(+1f) },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("A+", style = CaptionStyle, color = textSecondary)
                    }

                    Spacer(modifier = Modifier.width(Spacing.S1))

                    // Fullscreen Button
                    IconButton(
                        onClick = { onShowSnackbar("Full screen mode") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_maximize),
                            contentDescription = "Fullscreen",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.S1))

                    // Run Button
                    IconButton(
                        onClick = {
                            viewModel.selectTab(1) // Switch to preview tab
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Primary, RoundedCornerShape(R_SM))
                            .testTag("run_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Run",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Main Editor / Preview View
            if (uiState.selectedTab == 0) {
                // Code Tab
                Column(modifier = Modifier.weight(1f)) {
                    SymbolBar(
                        onInsertSymbol = { symbol -> viewModel.insertSymbol(symbol) }
                    )

                    CodeEditor(
                        textFieldValue = uiState.textFieldValue,
                        onValueChange = { viewModel.onTextFieldValueChange(it) },
                        fontSp = uiState.fontSp,
                        currentLineIndex = uiState.currentLineIndex,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Preview Tab
                RunPreview(
                    type = uiState.type,
                    content = uiState.textFieldValue.text,
                    modifier = Modifier.weight(1f),
                    onToast = onShowSnackbar
                )
            }
        }

        // Discard Confirmation Dialog
        ConfirmDialog(
            show = showDiscardDialog,
            title = stringResource(R.string.confirm_discard_title),
            desc = "有未保存的更改，确定离开吗？",
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false },
            isDanger = true
        )
    }
}
