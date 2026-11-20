package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.PdfDocumentEntity
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.FavoriteGold
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.util.BengaliFormatters
import java.io.File

@Composable
fun PdfDocumentCard(
    document: PdfDocumentEntity,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onViewInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_item_card_${document.id}")
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF Thumbnail preview
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LightBlueTint)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = document.thumbnailPath?.let { File(it) }
                if (thumbFile != null && thumbFile.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(thumbFile),
                        contentDescription = "${document.name} প্রিভিউ",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = "পিডিএফ আইকন",
                        tint = RoyalBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepInk,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (document.isFavorite) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "পছন্দের তালিকা",
                            tint = FavoriteGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = BengaliFormatters.formatShortDate(document.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk,
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = LightBlueTint,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = BengaliFormatters.formatFileSize(document.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = BengaliFormatters.formatPageCount(document.pageCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedInk
                    )
                    if (document.isCompressed) {
                        Surface(
                            color = SuccessGreenContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "সংকুচিত",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pdf_menu_button_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "ফাইল মেনু",
                        tint = MutedInk
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = CardSurface
                ) {
                    DropdownMenuItem(
                        text = { Text("১. পিডিএফ খুলুন", color = DeepInk) },
                        leadingIcon = {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = RoyalBlue)
                        },
                        onClick = {
                            menuExpanded = false
                            onOpen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("২. নাম পরিবর্তন করুন", color = DeepInk) },
                        leadingIcon = {
                            Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, tint = RoyalBlue)
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (document.isFavorite) "৩. পছন্দের তালিকা থেকে সরান"
                                else "৩. পছন্দের তালিকায় যুক্ত করুন",
                                color = DeepInk
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (document.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = null,
                                tint = FavoriteGold
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("৪. শেয়ার করুন", color = DeepInk) },
                        leadingIcon = {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = RoyalBlue)
                        },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("৫. ফাইলের তথ্য দেখুন", color = DeepInk) },
                        leadingIcon = {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MutedInk)
                        },
                        onClick = {
                            menuExpanded = false
                            onViewInfo()
                        }
                    )
                    HorizontalDivider(color = SubtleBorder)
                    DropdownMenuItem(
                        text = { Text("৬. মুছে ফেলুন", color = ErrorCrimson) },
                        leadingIcon = {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = ErrorCrimson)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
