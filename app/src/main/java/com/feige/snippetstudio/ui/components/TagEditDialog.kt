package com.feige.snippetstudio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feige.snippetstudio.R
import com.feige.snippetstudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagEditDialog(
    show: Boolean,
    initialTags: List<String>,
    allAvailableTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    if (!show) return

    val tc = LocalThemeColors.current

    val currentTags = remember(show, initialTags) {
        mutableStateListOf<String>().apply { addAll(initialTags) }
    }
    var tagInput by remember { mutableStateOf("") }

    val addTagAction = {
        val trimmed = tagInput.trim().removePrefix("#").trim()
        if (trimmed.isNotEmpty() && !currentTags.contains(trimmed)) {
            currentTags.add(trimmed)
            tagInput = ""
        }
    }

    val candidateTags = remember(allAvailableTags, currentTags.toList()) {
        allAvailableTags.filter { !currentTags.contains(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tag,
                    contentDescription = null,
                    tint = tc.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "编辑标签",
                    style = SectionTitleStyle,
                    color = tc.text
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "已选标签",
                    style = CaptionStyle,
                    color = tc.primary,
                    fontWeight = FontWeight.Bold
                )

                if (currentTags.isEmpty()) {
                    Text(
                        text = "暂未选择标签，点击下方候选或手动输入添加",
                        style = CaptionStyle,
                        color = tc.text2,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentTags.forEach { tag ->
                            Surface(
                                color = C_TagBg,
                                shape = RoundedCornerShape(R_SM)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "# $tag",
                                        style = ListTitleStyle,
                                        color = C_Tag,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove Tag",
                                        tint = C_Tag,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { currentTags.remove(tag) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (candidateTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "快捷点选现有/预设标签",
                        style = CaptionStyle,
                        color = tc.text2
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        candidateTags.forEach { tag ->
                            Surface(
                                color = tc.surface2,
                                shape = RoundedCornerShape(R_SM),
                                modifier = Modifier.clickable { currentTags.add(tag) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = tc.text2,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "# $tag",
                                        style = CaptionStyle,
                                        color = tc.text
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("输入新自定义标签") },
                        placeholder = { Text("例如: UI, API, 工具") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addTagAction() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tag_input_field")
                    )

                    IconButton(
                        onClick = addTagAction,
                        enabled = tagInput.trim().isNotEmpty(),
                        modifier = Modifier.testTag("tag_add_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Tag",
                            tint = if (tagInput.trim().isNotEmpty()) tc.primary else tc.text2
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(currentTags.toList())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = tc.primary),
                shape = AppShapes.small,
                modifier = Modifier.testTag("tag_save_btn")
            ) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("tag_cancel_btn")
            ) {
                Text(stringResource(R.string.common_cancel), style = ListTitleStyle)
            }
        },
        shape = AppShapes.large
    )
}
