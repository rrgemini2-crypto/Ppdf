package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.example.data.local.PdfDocumentEntity
import com.example.data.model.CompressionLevel
import com.example.data.model.CompressionResult
import com.example.ui.CompressionUiState
import com.example.ui.components.LightBlueHeader
import com.example.ui.components.SelectPdfDocumentDialog
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorCrimsonContainer
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters
import java.io.File

@Composable
fun PdfCompressScreen(
    uiState: CompressionUiState,
    appDocuments: List<PdfDocumentEntity>,
    onPickFromMobile: () -> Unit,
    onSelectAppDocument: (PdfDocumentEntity) -> Unit,
    onSelectLevel: (CompressionLevel) -> Unit,
    onStartCompression: () -> Unit,
    onCancelCompression: () -> Unit,
    onOpenCompressedPdf: (Long) -> Unit,
    onSaveCompressedCopy: (File, String) -> Unit,
    onShareCompressedFile: (File, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showAppPdfPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "পিডিএফ সংকুচিত করা",
            subtitle = "মান ঠিক রেখে পিডিএফের আকার ছোট করুন",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step 1: Select PDF
                Text(
                    text = "১. পিডিএফ নির্বাচন করুন",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepInk,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickFromMobile,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("compress_pick_mobile"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RoyalBlue)
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = RoyalBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("মোবাইল থেকে", color = RoyalBlue)
                    }
                    OutlinedButton(
                        onClick = { showAppPdfPicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("compress_pick_app"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RoyalBlue)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = RoyalBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("অ্যাপের ফাইল", color = RoyalBlue)
                    }
                }

                uiState.selectedPdf?.let { selected ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LightBlueTint,
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selected.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepInk,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "বর্তমান আকার: ${BengaliFormatters.formatFileSize(selected.sizeBytes)} • মোট পৃষ্ঠা: ${BengaliFormatters.toBengaliDigits(selected.pageCount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk
                                )
                            }
                        }
                    }

                    // Step 2: Compression Level
                    Text(
                        text = "২. সংকোচনের মান নির্বাচন করুন",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepInk,
                        fontWeight = FontWeight.Bold
                    )

                    CompressionLevel.entries.forEach { level ->
                        val isSelected = uiState.level == level
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !uiState.isCompressing) { onSelectLevel(level) }
                                .testTag("compress_level_${level.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) LightBlueTint else CardSurface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) RoyalBlue else SubtleBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { if (!uiState.isCompressing) onSelectLevel(level) },
                                    colors = RadioButtonDefaults.colors(selectedColor = RoyalBlue)
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = level.titleBn,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DeepInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = level.descriptionBn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedInk
                                    )
                                }
                            }
                        }
                    }
                }

                // Progress indicator while compressing
                if (uiState.isCompressing) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LightBlueTint,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "সংকোচন চলছে... (পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(uiState.currentPage)} / ${BengaliFormatters.toBengaliDigits(uiState.totalPages)})",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepInk,
                                fontWeight = FontWeight.Bold
                            )
                            val frac = if (uiState.totalPages > 0) {
                                uiState.currentPage.toFloat() / uiState.totalPages
                            } else 0f
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier.fillMaxWidth(),
                                color = RoyalBlue
                            )
                            TextButton(
                                onClick = onCancelCompression,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("বাতিল করুন", color = ErrorCrimson)
                            }
                        }
                    }
                }

                // Error Banner
                uiState.errorBn?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ErrorCrimsonContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = ErrorCrimson,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // Result Card: Before vs After Comparison
                when (val res = uiState.result) {
                    null -> Unit
                    is CompressionResult.Success -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SuccessGreenContainer,
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "সংকোচন সম্পন্ন হয়েছে! (${BengaliFormatters.toBengaliDigits(res.savedPercent)}% কমেছে)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                ComparisonStatRow("আগের ফাইল", BengaliFormatters.formatFileSize(res.originalSizeBytes))
                                ComparisonStatRow("নতুন ফাইল", BengaliFormatters.formatFileSize(res.compressedSizeBytes))
                                ComparisonStatRow("কমেছে", "${BengaliFormatters.formatFileSize(res.savedBytes)} (${BengaliFormatters.toBengaliDigits(res.savedPercent)}%)")
                                Text(
                                    text = "নোট: আপনার মূল ফাইলটি অপরিবর্তিত রাখা হয়েছে।",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedInk
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    res.savedDocumentId?.let { docId ->
                                        Button(
                                            onClick = { onOpenCompressedPdf(docId) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("খুলুন", maxLines = 1)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onSaveCompressedCopy(File(res.outputFilePath), res.outputFileName)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("সংরক্ষণ", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onShareCompressedFile(File(res.outputFilePath), res.outputFileName)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("শেয়ার", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    is CompressionResult.AlreadyCompressed -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarningAmberContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = WarningAmber)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "এই পিডিএফ ফাইলটি ইতিমধ্যে সংকুচিত অবস্থায় আছে, তাই আর সংকোচন করা সম্ভব নয়।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepInk
                                )
                            }
                        }
                    }

                    is CompressionResult.Error -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ErrorCrimsonContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = res.messageBn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorCrimson,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            if (uiState.selectedPdf != null && !uiState.isCompressing) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onStartCompression,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_compress_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(Icons.Filled.Compress, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("পিডিএফ সংকুচিত করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAppPdfPicker) {
        SelectPdfDocumentDialog(
            title = "সংকোচনের জন্য পিডিএফ বাছুন",
            documents = appDocuments,
            allowExternalPick = false,
            onSelectDocument = onSelectAppDocument,
            onDismiss = { showAppPdfPicker = false }
        )
    }
}

@Composable
private fun ComparisonStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = DeepInk)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepInk,
            fontWeight = FontWeight.Bold
        )
    }
}
