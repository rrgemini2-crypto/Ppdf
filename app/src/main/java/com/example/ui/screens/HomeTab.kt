package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.FolderEntity
import com.example.data.local.PdfDocumentEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LightBlueHeader
import com.example.ui.components.PdfDocumentCard
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LightBlueHeaderBorder
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters

@Composable
fun HomeTab(
    documents: List<PdfDocumentEntity>,
    folders: List<FolderEntity>,
    selectedFolderId: Long?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectFolder: (Long?) -> Unit,
    onCreateFolderClick: () -> Unit,
    onDeleteFolderClick: (FolderEntity) -> Unit,
    onImageToPdfClick: () -> Unit,
    onScanDocumentClick: () -> Unit,
    onOpenPdf: (PdfDocumentEntity) -> Unit,
    onRenamePdf: (PdfDocumentEntity) -> Unit,
    onToggleFavorite: (PdfDocumentEntity) -> Unit,
    onSharePdf: (PdfDocumentEntity) -> Unit,
    onViewInfo: (PdfDocumentEntity) -> Unit,
    onDeletePdf: (PdfDocumentEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "সহজ পিডিএফ মেকার",
            subtitle = "অফলাইনে নিরাপদ পিডিএফ ও দলিল স্ক্যানার",
            actions = {
                IconButton(
                    onClick = onCreateFolderClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("home_create_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = "নতুন ফোল্ডার তৈরি",
                        tint = RoyalBlue
                    )
                }
            },
            bottomContent = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "নাম দিয়ে ফাইল খুঁজুন...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedInk
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "অনুসন্ধান",
                            tint = RoyalBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "মুছুন",
                                    tint = MutedInk
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = WhiteBackground,
                        unfocusedContainerColor = WhiteBackground,
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = LightBlueHeaderBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick action banner cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = "ছবি থেকে পিডিএফ",
                            subtitle = "গ্যালারি থেকে ছবি বাছুন",
                            icon = Icons.Filled.AddPhotoAlternate,
                            onClick = onImageToPdfClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_image_to_pdf")
                        )
                        QuickActionCard(
                            title = "দলিল স্ক্যান",
                            subtitle = "স্বয়ংক্রিয় কাগজ শনাক্ত",
                            icon = Icons.Filled.DocumentScanner,
                            onClick = onScanDocumentClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_doc_scan")
                        )
                    }
                }

                // Folder Filter Row
                if (folders.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedFolderId == null,
                                onClick = { onSelectFolder(null) },
                                label = { Text("সব ফাইল") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBlue,
                                    selectedLabelColor = WhiteBackground
                                )
                            )
                            folders.forEach { folder ->
                                FilterChip(
                                    selected = selectedFolderId == folder.id,
                                    onClick = { onSelectFolder(folder.id) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "ফোল্ডার মুছুন",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteFolderClick(folder) }
                                        )
                                    },
                                    label = { Text(folder.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalBlue,
                                        selectedLabelColor = WhiteBackground,
                                        selectedLeadingIconColor = WhiteBackground,
                                        selectedTrailingIconColor = WhiteBackground
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "আপনার পিডিএফ ফাইলসমূহ",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepInk,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${BengaliFormatters.toBengaliDigits(documents.size)}টি ফাইল",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedInk
                        )
                    }
                }

                if (documents.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = if (searchQuery.isNotBlank()) "কোনো মিল খুঁজে পাওয়া যায়নি" else "এখনো কোনো পিডিএফ তৈরি হয়নি",
                            subtitle = if (searchQuery.isNotBlank()) {
                                "অন্য নাম লিখে আবার খুঁজে দেখুন।"
                            } else {
                                "নিচের বাটনে চাপ দিয়ে গ্যালারির ছবি বা ক্যামেরার দলিল থেকে সহজেই পিডিএফ তৈরি করুন।"
                            },
                            actionLabel = if (searchQuery.isBlank()) "ছবি থেকে পিডিএফ তৈরি করুন" else null,
                            onActionClick = if (searchQuery.isBlank()) onImageToPdfClick else null
                        )
                    }
                } else {
                    items(documents, key = { it.id }) { doc ->
                        PdfDocumentCard(
                            document = doc,
                            onOpen = { onOpenPdf(doc) },
                            onRename = { onRenamePdf(doc) },
                            onToggleFavorite = { onToggleFavorite(doc) },
                            onShare = { onSharePdf(doc) },
                            onViewInfo = { onViewInfo(doc) },
                            onDelete = { onDeletePdf(doc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = LightBlueTint,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RoyalBlue,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = WhiteBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedInk,
                    maxLines = 1
                )
            }
        }
    }
}
