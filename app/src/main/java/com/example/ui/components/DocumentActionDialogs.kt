package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.FolderEntity
import com.example.data.local.PdfDocumentEntity
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters

@Composable
fun RenameDocumentDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBackground,
        title = {
            Text(
                text = "নাম পরিবর্তন করুন",
                style = MaterialTheme.typography.titleLarge,
                color = DeepInk
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("নতুন ফাইলের নাম") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_input_field")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier.testTag("rename_confirm_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = MutedInk)
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    documentName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBackground,
        title = {
            Text(
                text = "ফাইল মুছে ফেলবেন?",
                style = MaterialTheme.typography.titleLarge,
                color = DeepInk
            )
        },
        text = {
            Text(
                text = "আপনি কি নিশ্চিতভাবে \"$documentName\" ফাইলটি মুছে ফেলতে চান? এটি পুনরুদ্ধার করা যাবে না।",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson),
                modifier = Modifier.testTag("delete_confirm_button")
            ) {
                Text("হ্যাঁ, মুছে ফেলুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("না, রাখুন", color = DeepInk)
            }
        }
    )
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBackground,
        title = {
            Text(
                text = "নতুন ফোল্ডার তৈরি",
                style = MaterialTheme.typography.titleLarge,
                color = DeepInk
            )
        },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("ফোল্ডারের নাম (যেমন: অফিস, পরীক্ষা)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("folder_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier.testTag("create_folder_confirm_button")
            ) {
                Text("তৈরি করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = MutedInk)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentInfoDialog(
    document: PdfDocumentEntity,
    folders: List<FolderEntity>,
    onMoveToFolder: (Long?) -> Unit,
    onSaveToDevice: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBackground,
        title = {
            Text(
                text = "ফাইলের বিস্তারিত তথ্য",
                style = MaterialTheme.typography.titleLarge,
                color = DeepInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("ফাইলের নাম", document.name)
                InfoRow("ফাইলের আকার", BengaliFormatters.formatFileSize(document.sizeBytes))
                InfoRow("মোট পৃষ্ঠা", BengaliFormatters.formatPageCount(document.pageCount))
                InfoRow("তৈরির তারিখ", BengaliFormatters.formatDateTime(document.createdAt))
                InfoRow("সর্বশেষ খোলা", BengaliFormatters.formatDateTime(document.lastOpenedAt))

                if (folders.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = SubtleBorder)
                    Text(
                        text = "ফোল্ডারে রাখুন:",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepInk
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = document.folderId == null,
                            onClick = { onMoveToFolder(null) },
                            label = { Text("সাধারণ") }
                        )
                        folders.forEach { folder ->
                            FilterChip(
                                selected = document.folderId == folder.id,
                                onClick = { onMoveToFolder(folder.id) },
                                label = { Text(folder.name) }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = SubtleBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveToDevice,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("অন্য ফোল্ডারে", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onPrint,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("প্রিন্ট", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("শেয়ার করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = MutedInk)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MutedInk)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = DeepInk,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SelectPdfDocumentDialog(
    title: String,
    documents: List<PdfDocumentEntity>,
    allowExternalPick: Boolean = false,
    onPickExternal: () -> Unit = {},
    onSelectDocument: (PdfDocumentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBackground,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = DeepInk
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (allowExternalPick) {
                    Button(
                        onClick = {
                            onDismiss()
                            onPickExternal()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("মোবাইল থেকে পিডিএফ নির্বাচন করুন")
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (documents.isEmpty()) {
                    Text(
                        text = "অ্যাপের ভেতর এখনো কোনো পিডিএফ তৈরি করা হয়নি।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                } else {
                    Text(
                        text = "অ্যাপের তৈরি করা পিডিএফ:",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepInk
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(documents, key = { it.id }) { doc ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onSelectDocument(doc)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = LightBlueTint
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        tint = RoyalBlue
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DeepInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${BengaliFormatters.formatFileSize(doc.sizeBytes)} • ${BengaliFormatters.formatPageCount(doc.pageCount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = MutedInk)
            }
        }
    )
}
