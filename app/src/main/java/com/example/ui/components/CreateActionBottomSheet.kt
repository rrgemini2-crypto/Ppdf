package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepInk
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.WhiteBackground

enum class CreateSheetAction {
    IMPORT_FILES,
    CREATE_FOLDER,
    IMAGE_TO_PDF,
    SCAN_TO_PDF,
    WORD_TO_PDF,
    PDF_TO_WORD,
    TEXT_TO_PDF,
    PDF_TO_IMAGE,
    PDF_TO_LONG_IMAGE,
    COMPRESS_PDF,
    ORGANIZE_PDF_PAGES
}

private data class SheetConvertItem(
    val action: CreateSheetAction,
    val labelBn: String,
    val icon: ImageVector,
    val circleBg: Color,
    val iconTint: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActionBottomSheet(
    onDismiss: () -> Unit,
    onActionSelected: (CreateSheetAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val convertItems = listOf(
        SheetConvertItem(
            action = CreateSheetAction.IMAGE_TO_PDF,
            labelBn = "ছবি থেকে পিডিএফ",
            icon = Icons.Filled.AddPhotoAlternate,
            circleBg = Color(0xFFEEF4FF),
            iconTint = RoyalBlue
        ),
        SheetConvertItem(
            action = CreateSheetAction.SCAN_TO_PDF,
            labelBn = "স্ক্যান থেকে পিডিএফ",
            icon = Icons.Filled.DocumentScanner,
            circleBg = Color(0xFFEAFBF3),
            iconTint = Color(0xFF059669)
        ),
        SheetConvertItem(
            action = CreateSheetAction.WORD_TO_PDF,
            labelBn = "ওয়ার্ড থেকে পিডিএফ",
            icon = Icons.Filled.Description,
            circleBg = Color(0xFFEAF6FF),
            iconTint = Color(0xFF0284C7)
        ),
        SheetConvertItem(
            action = CreateSheetAction.PDF_TO_WORD,
            labelBn = "পিডিএফ থেকে ওয়ার্ড",
            icon = Icons.Filled.Article,
            circleBg = Color(0xFFFFF0F2),
            iconTint = Color(0xFFDC2626)
        ),
        SheetConvertItem(
            action = CreateSheetAction.TEXT_TO_PDF,
            labelBn = "টেক্সট থেকে পিডিএফ",
            icon = Icons.Filled.TextFields,
            circleBg = Color(0xFFFFF8E1),
            iconTint = Color(0xFFD97706)
        ),
        SheetConvertItem(
            action = CreateSheetAction.PDF_TO_IMAGE,
            labelBn = "পিডিএফ থেকে ছবি",
            icon = Icons.Filled.Collections,
            circleBg = Color(0xFFFFF1E8),
            iconTint = Color(0xFFEA580C)
        ),
        SheetConvertItem(
            action = CreateSheetAction.PDF_TO_LONG_IMAGE,
            labelBn = "পিডিএফ থেকে লম্বা ছবি",
            icon = Icons.Filled.ViewDay,
            circleBg = Color(0xFFF3F0FF),
            iconTint = Color(0xFF6D28D9)
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WhiteBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFFCBD5E1),
                width = 40.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section 1: ব্যবস্থাপনা (Manage)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ব্যবস্থাপনা",
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedInk,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ManagePastelCard(
                        title = "ফাইল ইমপোর্ট করুন",
                        icon = Icons.Filled.UploadFile,
                        containerColor = Color(0xFFF2F6FF),
                        borderColor = Color(0xFFDCE7FE),
                        iconBadgeColor = Color(0xFFE0EBFF),
                        iconTint = RoyalBlue,
                        onClick = {
                            onDismiss()
                            onActionSelected(CreateSheetAction.IMPORT_FILES)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_import_files")
                    )
                    ManagePastelCard(
                        title = "নতুন ফোল্ডার তৈরি করুন",
                        icon = Icons.Filled.CreateNewFolder,
                        containerColor = Color(0xFFF5F3FF),
                        borderColor = Color(0xFFE5DEFF),
                        iconBadgeColor = Color(0xFFE8E2FF),
                        iconTint = Color(0xFF6D28D9),
                        onClick = {
                            onDismiss()
                            onActionSelected(CreateSheetAction.CREATE_FOLDER)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_create_folder")
                    )
                }
            }

            // Section 2: রূপান্তর (Convert)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "রূপান্তর",
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedInk,
                    fontWeight = FontWeight.SemiBold
                )

                convertItems.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { item ->
                            ConvertCircleOption(
                                item = item,
                                onClick = {
                                    onDismiss()
                                    onActionSelected(item.action)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sheet_${item.action.name.lowercase()}")
                            )
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Section 3: পিডিএফ ব্যবস্থাপনা (PDF Management)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "পিডিএফ ব্যবস্থাপনা",
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedInk,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ManagePastelCard(
                        title = "পিডিএফ সংকুচিত করুন",
                        icon = Icons.Filled.Compress,
                        containerColor = Color(0xFFECFDF5),
                        borderColor = Color(0xFFD1FAE5),
                        iconBadgeColor = Color(0xFFD7F7E8),
                        iconTint = Color(0xFF059669),
                        onClick = {
                            onDismiss()
                            onActionSelected(CreateSheetAction.COMPRESS_PDF)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_compress_pdf")
                    )
                    ManagePastelCard(
                        title = "পিডিএফ পৃষ্ঠা সাজান",
                        icon = Icons.Filled.Reorder,
                        containerColor = Color(0xFFFFF7ED),
                        borderColor = Color(0xFFFED7AA),
                        iconBadgeColor = Color(0xFFFFECD6),
                        iconTint = Color(0xFFEA580C),
                        onClick = {
                            onDismiss()
                            onActionSelected(CreateSheetAction.ORGANIZE_PDF_PAGES)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_organize_pages")
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagePastelCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    borderColor: Color,
    iconBadgeColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 68.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBadgeColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = DeepInk,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConvertCircleOption(
    item: SheetConvertItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = item.circleBg,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.labelBn,
                    tint = item.iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.labelBn,
            style = MaterialTheme.typography.labelSmall,
            color = DeepInk,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
