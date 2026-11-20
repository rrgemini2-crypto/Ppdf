package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.PdfDocumentEntity
import com.example.ui.PdfToLongImageUiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LightBlueHeader
import com.example.ui.components.SelectPdfDocumentDialog
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters
import com.example.util.FileActionsHelper
import com.example.util.ImageExportQuality
import com.example.util.ImageOutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfToLongImageScreen(
    uiState: PdfToLongImageUiState,
    appDocuments: List<PdfDocumentEntity>,
    onPickFromMobile: () -> Unit,
    onSelectAppDocument: (PdfDocumentEntity) -> Unit,
    onTogglePage: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onSelectFormat: (ImageOutputFormat) -> Unit,
    onSelectQuality: (ImageExportQuality) -> Unit,
    onGenerateLongImage: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val saveToFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        val res = uiState.stitchedResult
        if (treeUri != null && res != null && res.outputFile.exists()) {
            scope.launch {
                isExporting = true
                val savedOk = withContext(Dispatchers.IO) {
                    val bmp = BitmapFactory.decodeFile(res.outputFile.absolutePath) ?: return@withContext false
                    val saveResult = FileActionsHelper.saveBitmapToFolderUri(
                        context = context,
                        bitmap = bmp,
                        treeUri = treeUri,
                        fileNameWithoutExt = "${uiState.sourceName}_লম্বা_ছবি",
                        format = uiState.outputFormat,
                        qualityPercent = uiState.outputQuality.jpegQuality
                    )
                    bmp.recycle()
                    saveResult.isSuccess
                }
                isExporting = false
                Toast.makeText(
                    context,
                    if (savedOk) "নির্বাচিত ফোল্ডারে সংরক্ষিত হয়েছে।" else "ফোল্ডারে সংরক্ষণ করা যায়নি।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "পিডিএফ থেকে লম্বা ছবি",
            subtitle = "নির্বাচিত পৃষ্ঠাগুলো উপর থেকে নিচে জোড়া দিয়ে একটি লম্বা ছবি বানান",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPickFromMobile,
                    enabled = !uiState.isLoadingPages && !uiState.isStitching,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("মোবাইল থেকে", color = RoyalBlue)
                }
                OutlinedButton(
                    onClick = { showPicker = true },
                    enabled = !uiState.isLoadingPages && !uiState.isStitching,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("অ্যাপের ফাইল", color = RoyalBlue)
                }
            }

            when {
                uiState.isLoadingPages -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RoyalBlue)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "পৃষ্ঠাগুলো পড়া হচ্ছে (${BengaliFormatters.toBengaliDigits(uiState.currentPage)} / ${BengaliFormatters.toBengaliDigits(uiState.totalPages)})...",
                                color = MutedInk
                            )
                        }
                    }
                }

                uiState.errorBn != null -> {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView(title = "সমস্যা হয়েছে", subtitle = uiState.errorBn)
                    }
                }

                uiState.pages.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView(
                            title = "একটি পিডিএফ ফাইল বাছুন",
                            subtitle = "সব বা নির্দিষ্ট কিছু পৃষ্ঠা একসাথে জোড়া লাগিয়ে JPG বা PNG ফরম্যাটে লম্বা ছবি তৈরি করুন।"
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "পৃষ্ঠা নির্বাচন (${BengaliFormatters.toBengaliDigits(uiState.selectedIndices.size)} / ${BengaliFormatters.toBengaliDigits(uiState.pages.size)})",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepInk,
                                fontWeight = FontWeight.Bold
                            )
                            val allSelected = uiState.selectedIndices.size == uiState.pages.size
                            TextButton(onClick = { onSelectAll(!allSelected) }) {
                                Text(if (allSelected) "সব বাতিল" else "সব নির্বাচন", color = RoyalBlue)
                            }
                        }

                        // Horizontal scrollable page thumbnails for range/selection
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            itemsIndexed(uiState.pages) { idx, page ->
                                val selected = idx in uiState.selectedIndices
                                Box(
                                    modifier = Modifier
                                        .width(102.dp)
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LightBlueTint)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) RoyalBlue else SubtleBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onTogglePage(idx) }
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(File(page.imagePath)),
                                        contentDescription = BengaliFormatters.formatOrdinalPage(idx + 1),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        color = if (selected) RoyalBlue else WhiteBackground.copy(alpha = 0.85f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (selected) {
                                                Icon(Icons.Filled.Check, contentDescription = null, tint = WhiteBackground, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Surface(
                                        color = DeepInk.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "${BengaliFormatters.toBengaliDigits(idx + 1)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WhiteBackground,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Format & Quality Options
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = LightBlueTint),
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ছবির ফরম্যাট ও মান",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepInk,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ImageOutputFormat.entries.forEach { fmt ->
                                        FilterChip(
                                            selected = uiState.outputFormat == fmt,
                                            onClick = { onSelectFormat(fmt) },
                                            label = { Text(fmt.titleBn) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = RoyalBlue,
                                                selectedLabelColor = WhiteBackground
                                            )
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    ImageExportQuality.entries.forEach { q ->
                                        FilterChip(
                                            selected = uiState.outputQuality == q,
                                            onClick = { onSelectQuality(q) },
                                            label = { Text(q.titleBn) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = RoyalBlue,
                                                selectedLabelColor = WhiteBackground
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.isStitching) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = RoyalBlue)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "পৃষ্ঠাগুলো জোড়া হচ্ছে (${BengaliFormatters.toBengaliDigits(uiState.stitchCurrent)} / ${BengaliFormatters.toBengaliDigits(uiState.stitchTotal)})...",
                                        color = MutedInk
                                    )
                                }
                            }
                        }

                        uiState.stitchedResult?.let { stitched ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SuccessGreenContainer),
                                border = BorderStroke(1.dp, SubtleBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "লম্বা ছবির প্রিভিউ প্রস্তুত (${BengaliFormatters.toBengaliDigits(stitched.pagesStitched)} পৃষ্ঠা)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DeepInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "মাপ: ${BengaliFormatters.toBengaliDigits(stitched.widthPx)} × ${BengaliFormatters.toBengaliDigits(stitched.heightPx)} পিক্সেল • আকার: ${BengaliFormatters.formatFileSize(stitched.sizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedInk
                                    )
                                }
                            }

                            // Scrollable stitched image preview
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WhiteBackground,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(stitched.outputFile),
                                    contentDescription = "লম্বা ছবির প্রিভিউ",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }

                    Surface(
                        color = CardSurface,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val stitched = uiState.stitchedResult
                        if (stitched == null) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Button(
                                    onClick = onGenerateLongImage,
                                    enabled = uiState.selectedIndices.isNotEmpty() && !uiState.isStitching,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("long_image_generate_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                ) {
                                    Icon(Icons.Filled.ViewDay, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("লম্বা ছবি তৈরি করুন", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { FileActionsHelper.shareImages(context, listOf(stitched.outputFile)) },
                                    enabled = !isExporting,
                                    modifier = Modifier.weight(0.9f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("শেয়ার")
                                }
                                OutlinedButton(
                                    onClick = { saveToFolderLauncher.launch(null) },
                                    enabled = !isExporting,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ফোল্ডারে")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isExporting = true
                                            val ok = withContext(Dispatchers.IO) {
                                                val bmp = BitmapFactory.decodeFile(stitched.outputFile.absolutePath)
                                                    ?: return@withContext false
                                                val r = FileActionsHelper.saveBitmapToGalleryWithFormat(
                                                    context = context,
                                                    bitmap = bmp,
                                                    fileNameWithoutExt = "${uiState.sourceName}_লম্বা_ছবি",
                                                    format = uiState.outputFormat,
                                                    qualityPercent = uiState.outputQuality.jpegQuality
                                                )
                                                bmp.recycle()
                                                r.isSuccess
                                            }
                                            isExporting = false
                                            Toast.makeText(
                                                context,
                                                if (ok) "লম্বা ছবি গ্যালারিতে সংরক্ষিত হয়েছে।" else "গ্যালারিতে সংরক্ষণ করা যায়নি।",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    enabled = !isExporting,
                                    modifier = Modifier.weight(1.25f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                ) {
                                    Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("গ্যালারিতে")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        SelectPdfDocumentDialog(
            title = "লম্বা ছবির জন্য পিডিএফ বাছুন",
            documents = appDocuments,
            allowExternalPick = false,
            onSelectDocument = onSelectAppDocument,
            onDismiss = { showPicker = false }
        )
    }
}
