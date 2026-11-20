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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.PdfDocumentEntity
import com.example.ui.PdfToImageUiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LightBlueHeader
import com.example.ui.components.SelectPdfDocumentDialog
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
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
fun PdfToImageScreen(
    uiState: PdfToImageUiState,
    appDocuments: List<PdfDocumentEntity>,
    onPickFromMobile: () -> Unit,
    onSelectAppDocument: (PdfDocumentEntity) -> Unit,
    onToggleSelect: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onSelectFormat: (ImageOutputFormat) -> Unit,
    onSelectQuality: (ImageExportQuality) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val saveToFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null && uiState.selectedIndices.isNotEmpty()) {
            scope.launch {
                isSaving = true
                var savedCount = 0
                withContext(Dispatchers.IO) {
                    uiState.selectedIndices.sorted().forEach { idx ->
                        val f = File(uiState.pages[idx].imagePath)
                        val bmp = BitmapFactory.decodeFile(f.absolutePath)
                        if (bmp != null) {
                            val res = FileActionsHelper.saveBitmapToFolderUri(
                                context = context,
                                bitmap = bmp,
                                treeUri = treeUri,
                                fileNameWithoutExt = "${uiState.sourceName}_পৃষ্ঠা_${idx + 1}",
                                format = uiState.outputFormat,
                                qualityPercent = uiState.outputQuality.jpegQuality
                            )
                            bmp.recycle()
                            if (res.isSuccess) savedCount++
                        }
                    }
                }
                isSaving = false
                Toast.makeText(
                    context,
                    "${BengaliFormatters.toBengaliDigits(savedCount)}টি ছবি নির্বাচিত ফোল্ডারে সংরক্ষিত হয়েছে।",
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
            title = "পিডিএফ থেকে ছবি",
            subtitle = if (uiState.pages.isNotEmpty()) {
                "${uiState.sourceName} • নির্বাচিত: ${BengaliFormatters.toBengaliDigits(uiState.selectedIndices.size)} / ${BengaliFormatters.toBengaliDigits(uiState.pages.size)}"
            } else {
                "পিডিএফের প্রতিটি পৃষ্ঠা JPG বা PNG হিসেবে সংরক্ষণ করুন"
            },
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
                uiState.isLoading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RoyalBlue)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "পৃষ্ঠাগুলো ছবিতে রূপান্তর হচ্ছে (${BengaliFormatters.toBengaliDigits(uiState.currentPage)} / ${BengaliFormatters.toBengaliDigits(uiState.totalPages)})...",
                                color = MutedInk
                            )
                        }
                    }
                }

                uiState.errorBn != null -> {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView(
                            title = "সমস্যা হয়েছে",
                            subtitle = uiState.errorBn
                        )
                    }
                }

                uiState.pages.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView(
                            title = "একটি পিডিএফ ফাইল বাছুন",
                            subtitle = "উপরের বাটন থেকে মোবাইল বা অ্যাপের পিডিএফ নির্বাচন করলে সব পৃষ্ঠা ছবি আকারে দেখাবে।"
                        )
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সংরক্ষণ করার পৃষ্ঠা নির্বাচন করুন",
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepInk,
                            fontWeight = FontWeight.Bold
                        )
                        val allSelected = uiState.selectedIndices.size == uiState.pages.size
                        TextButton(onClick = { onSelectAll(!allSelected) }) {
                            Text(if (allSelected) "সব বাতিল" else "সব নির্বাচন", color = RoyalBlue)
                        }
                    }

                    // Format & Quality selector bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                        Spacer(Modifier.width(4.dp))
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

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(uiState.pages) { idx, item ->
                            val selected = idx in uiState.selectedIndices
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.74f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(LightBlueTint)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) RoyalBlue else SubtleBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onToggleSelect(idx) }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(File(item.imagePath)),
                                    contentDescription = BengaliFormatters.formatOrdinalPage(idx + 1),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = if (selected) RoyalBlue else WhiteBackground.copy(alpha = 0.85f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (selected) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = WhiteBackground, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Surface(
                                    color = DeepInk.copy(alpha = 0.72f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = BengaliFormatters.formatOrdinalPage(idx + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WhiteBackground,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val files = uiState.selectedIndices.sorted().map { File(uiState.pages[it].imagePath) }
                                FileActionsHelper.shareImages(context, files)
                            },
                            enabled = uiState.selectedIndices.isNotEmpty() && !isSaving,
                            modifier = Modifier.weight(0.9f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("শেয়ার", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { saveToFolderLauncher.launch(null) },
                            enabled = uiState.selectedIndices.isNotEmpty() && !isSaving,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ফোল্ডারে", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    var savedCount = 0
                                    withContext(Dispatchers.IO) {
                                        uiState.selectedIndices.sorted().forEach { idx ->
                                            val f = File(uiState.pages[idx].imagePath)
                                            val bmp = BitmapFactory.decodeFile(f.absolutePath)
                                            if (bmp != null) {
                                                val res = FileActionsHelper.saveBitmapToGalleryWithFormat(
                                                    context = context,
                                                    bitmap = bmp,
                                                    fileNameWithoutExt = "${uiState.sourceName}_পৃষ্ঠা_${idx + 1}",
                                                    format = uiState.outputFormat,
                                                    qualityPercent = uiState.outputQuality.jpegQuality
                                                )
                                                bmp.recycle()
                                                if (res.isSuccess) savedCount++
                                            }
                                        }
                                    }
                                    isSaving = false
                                    Toast.makeText(
                                        context,
                                        "${BengaliFormatters.toBengaliDigits(savedCount)}টি ছবি গ্যালারিতে সংরক্ষিত হয়েছে।",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = uiState.selectedIndices.isNotEmpty() && !isSaving,
                            modifier = Modifier.weight(1.25f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isSaving) "হচ্ছে..." else "গ্যালারিতে", maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        SelectPdfDocumentDialog(
            title = "পিডিএফ নির্বাচন করুন",
            documents = appDocuments,
            allowExternalPick = false,
            onSelectDocument = onSelectAppDocument,
            onDismiss = { showPicker = false }
        )
    }
}
