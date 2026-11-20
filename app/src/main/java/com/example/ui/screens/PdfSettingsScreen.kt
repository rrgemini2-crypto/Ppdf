package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.FolderEntity
import com.example.data.model.ImagePlacement
import com.example.data.model.ImageQuality
import com.example.data.model.PageMargin
import com.example.data.model.PageOrientation
import com.example.data.model.PdfBuildConfig
import com.example.data.model.PdfPageSize
import com.example.engine.PdfGenerator
import com.example.ui.ProgressUiState
import com.example.ui.components.LightBlueHeader
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfSettingsScreen(
    pageCount: Int,
    config: PdfBuildConfig,
    folders: List<FolderEntity>,
    progressState: ProgressUiState,
    onUpdateConfig: ((PdfBuildConfig) -> PdfBuildConfig) -> Unit,
    onBuildPdf: () -> Unit,
    onCancelBuild: () -> Unit,
    onResetProgress: () -> Unit,
    onOpenGeneratedPdf: (Long) -> Unit,
    onShareGeneratedPdf: (Long) -> Unit,
    onBackToHome: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val estimatedSizeBytes = remember(pageCount, config) {
        PdfGenerator.estimateSizeBytes(pageCount, config)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "পিডিএফ তৈরির সেটিংস",
            subtitle = "মোট পৃষ্ঠা: ${BengaliFormatters.toBengaliDigits(pageCount)} • সম্ভাব্য আকার: ${BengaliFormatters.formatFileSize(estimatedSizeBytes)}",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // File Name
                OutlinedTextField(
                    value = config.fileName,
                    onValueChange = { next -> onUpdateConfig { it.copy(fileName = next) } },
                    label = { Text("পিডিএফের নাম") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_settings_filename_input")
                )

                // Estimated Size Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LightBlueTint,
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("পিডিএফের সম্ভাব্য আকার", style = MaterialTheme.typography.labelMedium, color = MutedInk)
                            Text(
                                text = BengaliFormatters.formatFileSize(estimatedSizeBytes),
                                style = MaterialTheme.typography.titleMedium,
                                color = RoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("মোট পৃষ্ঠা", style = MaterialTheme.typography.labelMedium, color = MutedInk)
                            Text(
                                text = BengaliFormatters.formatPageCount(pageCount),
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Page Size
                SettingSectionTitle("পৃষ্ঠার আকার")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PdfPageSize.entries.forEach { size ->
                        FilterChip(
                            selected = config.pageSize == size,
                            onClick = { onUpdateConfig { it.copy(pageSize = size) } },
                            label = { Text(size.titleBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = WhiteBackground
                            )
                        )
                    }
                }
                if (config.pageSize == PdfPageSize.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = config.customWidthMm.toString(),
                            onValueChange = { txt ->
                                val v = txt.filter { it.isDigit() }.toIntOrNull() ?: 210
                                onUpdateConfig { it.copy(customWidthMm = v) }
                            },
                            label = { Text("প্রস্থ (মিমি)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = config.customHeightMm.toString(),
                            onValueChange = { txt ->
                                val v = txt.filter { it.isDigit() }.toIntOrNull() ?: 297
                                onUpdateConfig { it.copy(customHeightMm = v) }
                            },
                            label = { Text("উচ্চতা (মিমি)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Orientation
                SettingSectionTitle("পৃষ্ঠার দিক")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PageOrientation.entries.forEach { orient ->
                        FilterChip(
                            selected = config.orientation == orient,
                            onClick = { onUpdateConfig { it.copy(orientation = orient) } },
                            label = { Text(orient.titleBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = WhiteBackground
                            )
                        )
                    }
                }

                // Margin
                SettingSectionTitle("পৃষ্ঠার মার্জিন")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PageMargin.entries.forEach { m ->
                        FilterChip(
                            selected = config.margin == m,
                            onClick = { onUpdateConfig { it.copy(margin = m) } },
                            label = { Text(m.titleBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = WhiteBackground
                            )
                        )
                    }
                }

                // Image Placement
                SettingSectionTitle("ছবির অবস্থান ও আকার")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ImagePlacement.entries.forEach { p ->
                        FilterChip(
                            selected = config.placement == p,
                            onClick = { onUpdateConfig { it.copy(placement = p) } },
                            label = { Text(p.titleBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = WhiteBackground
                            )
                        )
                    }
                }

                // Image Quality
                SettingSectionTitle("ছবির গুণগত মান")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ImageQuality.entries.forEach { q ->
                        FilterChip(
                            selected = config.quality == q,
                            onClick = { onUpdateConfig { it.copy(quality = q) } },
                            label = { Text(q.titleBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = WhiteBackground
                            )
                        )
                    }
                }

                // Compression toggle
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WhiteBackground,
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ফাইল সংকুচিত করুন",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepInk,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "পিডিএফের আকার ছোট রাখতে সাহায্য করবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk
                            )
                        }
                        Switch(
                            checked = config.compressOutput,
                            onCheckedChange = { checked ->
                                onUpdateConfig { it.copy(compressOutput = checked) }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = RoyalBlue)
                        )
                    }
                }

                // Folder Selection
                if (folders.isNotEmpty()) {
                    SettingSectionTitle("ফোল্ডার নির্বাচন (ঐচ্ছিক)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = config.folderId == null,
                            onClick = { onUpdateConfig { it.copy(folderId = null) } },
                            label = { Text("সাধারণ") }
                        )
                        folders.forEach { f ->
                            FilterChip(
                                selected = config.folderId == f.id,
                                onClick = { onUpdateConfig { it.copy(folderId = f.id) } },
                                label = { Text(f.name) }
                            )
                        }
                    }
                }
            }

            // Primary Royal Blue Create Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WhiteBackground,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onBuildPdf,
                        enabled = config.fileName.isNotBlank() && pageCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("create_pdf_final_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "পিডিএফ তৈরি করুন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Build Progress / Result Dialogs
    when (progressState) {
        ProgressUiState.Idle -> Unit
        is ProgressUiState.Running -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = WhiteBackground,
                title = {
                    Text("পিডিএফ তৈরি হচ্ছে", style = MaterialTheme.typography.titleLarge, color = DeepInk)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(progressState.messageBn, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                        val fraction = if (progressState.total > 0) {
                            progressState.current.toFloat() / progressState.total
                        } else 0f
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth(),
                            color = RoyalBlue
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onCancelBuild) {
                        Text("বাতিল করুন", color = MutedInk)
                    }
                }
            )
        }
        is ProgressUiState.Completed -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = WhiteBackground,
                icon = {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                },
                title = {
                    Text("পিডিএফ তৈরি সফল হয়েছে!", color = DeepInk, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "\"${progressState.documentName}\" ফাইলটি আপনার মোবাইলে নিরাপদে সংরক্ষণ করা হয়েছে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedInk
                        )
                        OutlinedButton(
                            onClick = { onShareGeneratedPdf(progressState.documentId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("শেয়ার করুন")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onOpenGeneratedPdf(progressState.documentId) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.testTag("open_created_pdf_button")
                    ) {
                        Text("পিডিএফ খুলুন")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onBackToHome) {
                        Text("হোমে যান", color = DeepInk)
                    }
                }
            )
        }
        is ProgressUiState.Failed -> {
            AlertDialog(
                onDismissRequest = onResetProgress,
                containerColor = WhiteBackground,
                title = { Text("সমস্যা হয়েছে", color = DeepInk) },
                text = { Text(progressState.errorBn, color = MutedInk) },
                confirmButton = {
                    Button(
                        onClick = onResetProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Text("ঠিক আছে")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = DeepInk,
        fontWeight = FontWeight.Bold
    )
}
