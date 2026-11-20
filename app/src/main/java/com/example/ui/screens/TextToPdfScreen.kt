package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PdfPageSize
import com.example.engine.TextAlignOption
import com.example.engine.TextToPdfEngine
import com.example.ui.TextToPdfUiState
import com.example.ui.components.LightBlueHeader
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
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextToPdfScreen(
    uiState: TextToPdfUiState,
    onUpdateState: ((TextToPdfUiState) -> TextToPdfUiState) -> Unit,
    onImportTxtFile: () -> Unit,
    onCreatePdf: () -> Unit,
    onOpenCreatedPdf: (Long) -> Unit,
    onShareCreatedPdf: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showPreview by remember { mutableStateOf(false) }

    val previewPages = remember(
        uiState.title,
        uiState.bodyText,
        uiState.fontSizePt,
        uiState.pageSize,
        showPreview
    ) {
        if (showPreview) {
            TextToPdfEngine.paginateTextForPreview(
                title = uiState.title,
                bodyText = uiState.bodyText,
                fontSizePt = uiState.fontSizePt,
                pageSize = uiState.pageSize
            )
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "টেক্সট থেকে পিডিএফ",
            subtitle = "বাংলা বা ইংরেজি লেখা টাইপ/পেস্ট করে পিডিএফ বানান",
            onBackClick = onBack,
            actions = {
                OutlinedButton(
                    onClick = onImportTxtFile,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, RoyalBlue),
                    modifier = Modifier.testTag("text_pdf_import_txt")
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(".TXT ফাইল", color = RoyalBlue, style = MaterialTheme.typography.labelMedium)
                }
            }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.createdDocId != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreenContainer),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "পিডিএফ সংরক্ষিত হয়েছে: ${uiState.createdDocName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepInk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onOpenCreatedPdf(uiState.createdDocId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("খুলুন")
                                }
                                OutlinedButton(
                                    onClick = { onShareCreatedPdf(uiState.createdDocId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("শেয়ার")
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

                OutlinedTextField(
                    value = uiState.fileName,
                    onValueChange = { v -> onUpdateState { it.copy(fileName = v) } },
                    label = { Text("পিডিএফ ফাইলের নাম") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_pdf_filename_input")
                )

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { v -> onUpdateState { it.copy(title = v) } },
                    label = { Text("শিরোনাম (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: মাসিক হিসাব প্রতিবেদন") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_pdf_title_input")
                )

                OutlinedTextField(
                    value = uiState.bodyText,
                    onValueChange = { v -> onUpdateState { it.copy(bodyText = v) } },
                    label = { Text("মূল লেখা") },
                    placeholder = { Text("এখানে বাংলা বা ইংরেজিতে আপনার লেখা লিখুন বা পেস্ট করুন...") },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 7,
                    maxLines = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 170.dp)
                        .testTag("text_pdf_body_input")
                )

                // Formatting options card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LightBlueTint),
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ফন্টের আকার",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${BengaliFormatters.toBengaliDigits(uiState.fontSizePt.toInt())} pt",
                                style = MaterialTheme.typography.labelLarge,
                                color = RoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = uiState.fontSizePt,
                            onValueChange = { s -> onUpdateState { it.copy(fontSizePt = s) } },
                            valueRange = 10f..22f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = RoyalBlue,
                                activeTrackColor = RoyalBlue
                            )
                        )

                        Text(
                            text = "লেখার বিন্যাস (অ্যালাইনমেন্ট)",
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepInk,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextAlignOption.entries.forEach { align ->
                                val selected = uiState.alignment == align
                                val icon = when (align) {
                                    TextAlignOption.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                    TextAlignOption.CENTER -> Icons.Filled.FormatAlignCenter
                                    TextAlignOption.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateState { it.copy(alignment = align) } },
                                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text(align.titleBn) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalBlue,
                                        selectedLabelColor = WhiteBackground,
                                        selectedLeadingIconColor = WhiteBackground
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "পৃষ্ঠার আকার",
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepInk,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(PdfPageSize.A4, PdfPageSize.LETTER, PdfPageSize.LEGAL, PdfPageSize.A5).forEach { size ->
                                FilterChip(
                                    selected = uiState.pageSize == size,
                                    onClick = { onUpdateState { it.copy(pageSize = size) } },
                                    label = { Text(size.titleBn) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalBlue,
                                        selectedLabelColor = WhiteBackground
                                    )
                                )
                            }
                        }
                    }
                }

                // Live Preview Toggle
                OutlinedButton(
                    onClick = { showPreview = !showPreview },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (showPreview) "প্রিভিউ লুকান" else "পিডিএফ প্রিভিউ দেখুন",
                        color = RoyalBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showPreview) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "আনুমানিক পৃষ্ঠা বিন্যাস (${BengaliFormatters.formatPageCount(previewPages.size)})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MutedInk
                        )
                        val textAlign = when (uiState.alignment) {
                            TextAlignOption.LEFT -> TextAlign.Start
                            TextAlignOption.CENTER -> TextAlign.Center
                            TextAlignOption.RIGHT -> TextAlign.End
                        }
                        previewPages.take(4).forEach { page ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(8.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (page.showTitle && uiState.title.isNotBlank()) {
                                        Text(
                                            text = uiState.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DeepInk,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = textAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        HorizontalDivider(color = SubtleBorder)
                                    }
                                    Text(
                                        text = page.lines.joinToString("\n"),
                                        fontSize = (uiState.fontSizePt * 0.92f).sp,
                                        lineHeight = (uiState.fontSizePt * 1.35f).sp,
                                        color = DeepInk,
                                        textAlign = textAlign,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(page.pageNumber)} / ${BengaliFormatters.toBengaliDigits(previewPages.size)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedInk,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = CardSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = onCreatePdf,
                        enabled = !uiState.isBuilding && (uiState.bodyText.isNotBlank() || uiState.title.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("text_pdf_create_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        if (uiState.isBuilding) {
                            CircularProgressIndicator(color = WhiteBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("পিডিএফ তৈরি হচ্ছে...")
                        } else {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("পিডিএফ তৈরি ও সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
