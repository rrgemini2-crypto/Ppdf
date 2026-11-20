package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.LightBlueHeader
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorCrimsonContainer
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

enum class ToolAction {
    IMAGE_TO_PDF,
    CAMERA_TO_PDF,
    DOCUMENT_SCAN,
    MULTI_IMAGE_TO_PDF,
    PDF_TO_IMAGE,
    COMPRESS_PDF,
    EDIT_IMAGE,
    FILTER_IMAGE,
    CROP_IMAGE,
    ORGANIZE_PAGES,
    RENAME_PDF,
    SHARE_PDF,
    DELETE_PDF,
    CREATE_FOLDER,
    PRINT_PDF
}

private data class ToolTileData(
    val action: ToolAction,
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeBg: Color
)

@Composable
fun ToolsTab(
    onToolClick: (ToolAction) -> Unit
) {
    val createTools = listOf(
        ToolTileData(ToolAction.IMAGE_TO_PDF, "১. ছবি থেকে পিডিএফ", Icons.Filled.AddPhotoAlternate, RoyalBlue, LightBlueTint),
        ToolTileData(ToolAction.CAMERA_TO_PDF, "২. ক্যামেরা দিয়ে পিডিএফ", Icons.Filled.CameraAlt, RoyalBlue, LightBlueTint),
        ToolTileData(ToolAction.DOCUMENT_SCAN, "৩. দলিল স্ক্যান", Icons.Filled.DocumentScanner, RoyalBlue, LightBlueTint),
        ToolTileData(ToolAction.MULTI_IMAGE_TO_PDF, "৪. একাধিক ছবি থেকে পিডিএফ", Icons.Filled.Collections, RoyalBlue, LightBlueTint)
    )

    val convertTools = listOf(
        ToolTileData(ToolAction.PDF_TO_IMAGE, "৫. পিডিএফ থেকে ছবি", Icons.Filled.Image, SuccessGreen, SuccessGreenContainer),
        ToolTileData(ToolAction.COMPRESS_PDF, "৬. পিডিএফ সংকুচিত করা", Icons.Filled.Compress, SuccessGreen, SuccessGreenContainer)
    )

    val editTools = listOf(
        ToolTileData(ToolAction.EDIT_IMAGE, "৭. ছবি সম্পাদনা", Icons.Filled.Edit, WarningAmber, WarningAmberContainer),
        ToolTileData(ToolAction.FILTER_IMAGE, "৮. ছবি ফিল্টার", Icons.Filled.FilterVintage, WarningAmber, WarningAmberContainer),
        ToolTileData(ToolAction.CROP_IMAGE, "৯. ছবি কাটছাঁট", Icons.Filled.Crop, WarningAmber, WarningAmberContainer),
        ToolTileData(ToolAction.ORGANIZE_PAGES, "১০. পৃষ্ঠা সাজানো", Icons.Filled.Reorder, WarningAmber, WarningAmberContainer)
    )

    val manageTools = listOf(
        ToolTileData(ToolAction.RENAME_PDF, "১১. পিডিএফের নাম পরিবর্তন", Icons.Filled.DriveFileRenameOutline, RoyalBlue, LightBlueTint),
        ToolTileData(ToolAction.SHARE_PDF, "১২. ফাইল শেয়ার", Icons.Filled.Share, RoyalBlue, LightBlueTint),
        ToolTileData(ToolAction.DELETE_PDF, "১৩. ফাইল মুছে ফেলা", Icons.Filled.DeleteOutline, ErrorCrimson, ErrorCrimsonContainer),
        ToolTileData(ToolAction.CREATE_FOLDER, "১৪. নতুন ফোল্ডার তৈরি", Icons.Filled.CreateNewFolder, RoyalBlue, LightBlueTint),
        ToolTileData(ToolAction.PRINT_PDF, "১৫. পিডিএফ প্রিন্ট", Icons.Filled.Print, RoyalBlue, LightBlueTint)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "অন্যান্য সুবিধা",
            subtitle = "সব পিডিএফ তৈরি, রূপান্তর ও ব্যবস্থাপনা টুল"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    ToolCategorySection(
                        header = "তৈরি",
                        tools = createTools,
                        onToolClick = onToolClick
                    )
                }
                item {
                    ToolCategorySection(
                        header = "রূপান্তর",
                        tools = convertTools,
                        onToolClick = onToolClick
                    )
                }
                item {
                    ToolCategorySection(
                        header = "সম্পাদনা",
                        tools = editTools,
                        onToolClick = onToolClick
                    )
                }
                item {
                    ToolCategorySection(
                        header = "ব্যবস্থাপনা",
                        tools = manageTools,
                        onToolClick = onToolClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolCategorySection(
    header: String,
    tools: List<ToolTileData>,
    onToolClick: (ToolAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            color = DeepInk,
            fontWeight = FontWeight.Bold
        )
        val rows = tools.chunked(2)
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { tile ->
                    ToolTileCard(
                        tile = tile,
                        onClick = { onToolClick(tile.action) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tool_${tile.action.name.lowercase()}")
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ToolTileCard(
    tile: ToolTileData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(98.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, SubtleBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tile.badgeBg,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = tile.icon,
                        contentDescription = tile.title,
                        tint = tile.iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tile.title,
                style = MaterialTheme.typography.labelMedium,
                color = DeepInk,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
