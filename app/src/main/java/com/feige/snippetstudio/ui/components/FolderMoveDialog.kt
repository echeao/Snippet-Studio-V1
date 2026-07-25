package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderMoveDialog(
    show: Boolean,
    currentFolder: String,
    existingFolders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (targetFolder: String) -> Unit
) {
    if (!show) return

    val tc = LocalThemeColors.current
    var folderInput by remember(currentFolder) { mutableStateOf(currentFolder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "移动至文件夹", style = SectionTitleStyle)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.S2),
                verticalArrangement = Arrangement.spacedBy(Spacing.S3)
            ) {
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = { folderInput = it },
                    label = { Text("文件夹路径 (留空表示根目录)", style = CaptionStyle) },
                    placeholder = { Text("如: web/components", style = CaptionStyle) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_input")
                )

                if (existingFolders.isNotEmpty()) {
                    Text(text = "已存在文件夹：", style = CaptionStyle, color = tc.text2)
                    
                    Surface(
                        color = tc.surface2,
                        shape = RoundedCornerShape(R_MD),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(Spacing.S2)
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { folderInput = "" }
                                        .padding(horizontal = Spacing.S2, vertical = Spacing.S2)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = "Root Folder",
                                        tint = tc.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.S2))
                                    Text(text = "/ (根目录)", style = BodyStyle, color = tc.text)
                                }
                            }

                            items(existingFolders.filter { it.isNotBlank() }) { folder ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { folderInput = folder }
                                        .padding(horizontal = Spacing.S2, vertical = Spacing.S2)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = "Folder",
                                        tint = tc.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.S2))
                                    Text(text = folder, style = BodyStyle, color = tc.text)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(folderInput.trim())
                    onDismiss()
                },
                modifier = Modifier.testTag("folder_confirm_btn")
            ) {
                Text(text = stringResource(R.string.common_confirm), color = tc.primary, style = ListTitleStyle)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("folder_cancel_btn")
            ) {
                Text(text = stringResource(R.string.common_cancel), style = ListTitleStyle)
            }
        },
        shape = AppShapes.large
    )
}
