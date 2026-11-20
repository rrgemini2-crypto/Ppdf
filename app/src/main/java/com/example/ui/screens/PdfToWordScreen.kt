package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.PdfToWordUiState
import com.example.ui.components.EmptyStateView
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
fun PdfToWordScreen(
    uiState: PdfToWordUiState,
    appDocuments: List<PdfDocumentEntity>,
    onPickFromMobile: () -> Unit,
    onSelectAppDocument: (PdfDocumentEntity) -> Unit,
    onDownloadOcrPack: () -> Unit,
    onConvertToWord: () -> Unit,
    onSaveDocxCopy: (File, String) -> Unit,
    onShareDocx: (File, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showAppPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "পিডিএফ থেকে ওয়ার্ড",
            subtitle = "সম্পাদনাযোগ্য .DOCX ফাইল তৈরি (টেক্সট, ছক ও ওসিআর সহ)",
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
                    enabled = !uiState.isConverting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pdf_word_pick_mobile"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("মোবাইল থেকে", color = RoyalBlue)
                }
                OutlinedButton(
                    onClick = { showAppPicker = true },
                    enabled = !uiState.isConverting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pdf_word_pick_app"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("অ্যাপের ফাইল", color = RoyalBlue)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scanned PDF Bengali OCR pack status card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.hasBengaliOcrPack) SuccessGreenContainer else LightBlueTint
                    ),
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.hasBengaliOcrPack) Icons.Filled.CheckCircle else Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = if (uiState.hasBengaliOcrPack) SuccessGreen else RoyalBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (uiState.hasBengaliOcrPack) {
                                    "স্ক্যান করা বাংলা + ইংরেজি ওসিআর প্রস্তুত আছে"
                                } else {
                                    "স্ক্যান করা বাংলা পিডিএফের জন্য ওসিআর প্যাক"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!uiState.hasBengaliOcrPack) {
                            Text(
                                text = "সাধারণ ডিজিটাল পিডিএফ সম্পূর্ণ অফলাইনে রূপান্তর হয়। তবে ছবি-স্ক্যান করা বাংলা পৃষ্ঠার লেখা উদ্ধারের জন্য একবার প্যাকটি (~৫ এমবি) ডাউনলোড করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk
                            )
                            OutlinedButton(
                                onClick = onDownloadOcrPack,
                                enabled = !uiState.isDownloadingOcr && !uiState.isConverting,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, RoyalBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (uiState.isDownloadingOcr) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = RoyalBlue)
                                    Spacer(Modifier.width(8.dp))
                                    Text(uiState.ocrDownloadStatusBn.ifBlank { "ডাউনলোড হচ্ছে..." }, color = RoyalBlue)
                                } else {
                                    Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("বাংলা ওসিআর প্যাক ডাউনলোড করুন", color = RoyalBlue)
                                }
                            }
                        }
                    }
                }

                if (uiState.errorBn != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ErrorCrimsonContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.errorBn,
                            color = ErrorCrimson,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (uiState.sourceFile == null) {
                    EmptyStateView(
                        title = "পিডিএফ ফাইল বাছুন",
                        subtitle = "মোবাইল বা অ্যাপের যেকোনো পিডিএফ বেছে নিলে তা সম্পাদনাযোগ্য ওয়ার্ড (.docx) ফাইলে রূপান্তর হবে।"
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LightBlueTint),
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.sourceName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepInk,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "মোট পৃষ্ঠা: ${BengaliFormatters.formatPageCount(uiState.pageCount)} • ${BengaliFormatters.formatFileSize(uiState.sourceFile.length())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk
                                )
                            }
                        }
                    }

                    if (uiState.isConverting) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = {
                                        if (uiState.totalPages > 0) uiState.currentPage.toFloat() / uiState.totalPages else 0.4f
                                    },
                                    color = RoyalBlue,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${uiState.statusBn} (${BengaliFormatters.toBengaliDigits(uiState.currentPage)} / ${BengaliFormatters.toBengaliDigits(uiState.totalPages)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk
                                )
                            }
                        }
                    }

                    uiState.result?.let { res ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreenContainer),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "ওয়ার্ড (.docx) তৈরি সম্পন্ন হয়েছে",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DeepInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "ফাইল: ${res.displayTitle}.docx (${BengaliFormatters.formatFileSize(res.docxFile.length())})\n" +
                                        "পৃষ্ঠা: ${BengaliFormatters.toBengaliDigits(res.totalPages)} • " +
                                        "ছবি উদ্ধার: ${BengaliFormatters.toBengaliDigits(res.extractedImagesCount)}টি" +
                                        if (res.scannedPagesCount > 0) " • স্ক্যান/ওসিআর পৃষ্ঠা: ${BengaliFormatters.toBengaliDigits(res.scannedPagesCount)}টি" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepInk
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onSaveDocxCopy(res.docxFile, res.displayTitle) },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("ফোল্ডারে সেভ")
                                    }
                                    Button(
                                        onClick = { onShareDocx(res.docxFile, res.displayTitle) },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("শেয়ার (.docx)")
                                    }
                                }
                            }
                        }

                        if (res.hasLayoutLimitationWarning && res.warningsBn.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WarningAmberContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = res.warningsBn.joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DeepInk
                                    )
                                }
                            }
                        }

                        if (res.previewSnippet.isNotBlank()) {
                            Text(
                                text = "উদ্ধারকৃত লেখার অংশবিশেষ:",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepInk,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WhiteBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = res.previewSnippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepInk,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.sourceFile != null) {
                Surface(
                    color = CardSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Button(
                            onClick = onConvertToWord,
                            enabled = !uiState.isConverting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("pdf_word_convert_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Icon(Icons.Filled.Article, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isConverting) "ওয়ার্ড তৈরি হচ্ছে..." else "ওয়ার্ড (.DOCX) তৈরি করুন",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        SelectPdfDocumentDialog(
            title = "ওয়ার্ডে রূপান্তরের পিডিএফ বাছুন",
            documents = appDocuments,
            allowExternalPick = false,
            onSelectDocument = onSelectAppDocument,
            onDismiss = { showAppPicker = false }
        )
    }
}
