package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.engine.WordAlign
import com.example.engine.WordBlock
import com.example.ui.WordToPdfUiState
import com.example.ui.components.EmptyStateView
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
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters

@Composable
fun WordToPdfScreen(
    uiState: WordToPdfUiState,
    onPickWordFile: () -> Unit,
    onConvertToPdf: () -> Unit,
    onOpenCreatedPdf: (Long) -> Unit,
    onShareCreatedPdf: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "ওয়ার্ড থেকে পিডিএফ",
            subtitle = ".DOC এবং .DOCX ফাইলকে পরিষ্কার পিডিএফে রূপান্তর করুন",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedButton(
                    onClick = onPickWordFile,
                    enabled = !uiState.isParsing && !uiState.isConverting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("word_pdf_pick_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("মোবাইল থেকে DOC / DOCX ফাইল বাছুন", color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            when {
                uiState.isParsing -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RoyalBlue)
                            Spacer(Modifier.height(12.dp))
                            Text("ওয়ার্ড ফাইল পড়া ও বিশ্লেষণ হচ্ছে...", color = MutedInk)
                        }
                    }
                }

                uiState.errorBn != null -> {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView(
                            title = "ফাইল খোলা যায়নি",
                            subtitle = uiState.errorBn,
                            actionLabel = "অন্য ফাইল বাছুন",
                            onActionClick = onPickWordFile
                        )
                    }
                }

                uiState.parsedDocument == null -> {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView(
                            title = "ওয়ার্ড ফাইল নির্বাচন করুন",
                            subtitle = "উপরের বাটনে চাপ দিয়ে ডিভাইস থেকে .docx বা .doc ফাইল বাছুন। লেখা, টেবিল ও ছবি প্রিভিউ সহ পিডিএফ হবে।",
                            actionLabel = "DOC / DOCX বাছুন",
                            onActionClick = onPickWordFile
                        )
                    }
                }

                else -> {
                    val doc = uiState.parsedDocument
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // File summary banner
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
                                Icon(Icons.Filled.Description, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(30.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.sourceName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DeepInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ফরম্যাট: ${doc.sourceFormat} • মোট অংশ: ${BengaliFormatters.toBengaliDigits(doc.blocks.size)}টি",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedInk
                                    )
                                }
                            }
                        }

                        if (doc.warningsBn.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WarningAmberContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Filled.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = doc.warningsBn.joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DeepInk
                                    )
                                }
                            }
                        }

                        if (uiState.createdDocId != null) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
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
                                            text = "পিডিএফ তৈরি হয়েছে: ${uiState.createdDocName}",
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

                        Text(
                            text = "ডকুমেন্ট প্রিভিউ",
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepInk,
                            fontWeight = FontWeight.Bold
                        )

                        // Preview first 40 blocks nicely on a paper-like card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WhiteBackground,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                doc.blocks.take(40).forEach { block ->
                                    when (block) {
                                        is WordBlock.ParagraphBlock -> {
                                            val align = when (block.alignment) {
                                                WordAlign.CENTER -> TextAlign.Center
                                                WordAlign.RIGHT -> TextAlign.End
                                                else -> TextAlign.Start
                                            }
                                            Text(
                                                text = block.text,
                                                style = if (block.isHeading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (block.isBold || block.isHeading) FontWeight.Bold else FontWeight.Normal,
                                                fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                textAlign = align,
                                                color = DeepInk,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        is WordBlock.TableBlock -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, SubtleBorder, RoundedCornerShape(6.dp))
                                            ) {
                                                block.rows.take(10).forEachIndexed { rIdx, row ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(if (rIdx == 0) LightBlueTint else WhiteBackground)
                                                            .padding(6.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        row.forEach { cell ->
                                                            Text(
                                                                text = cell.ifBlank { "—" },
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                                color = DeepInk,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        is WordBlock.ImageBlock -> {
                                            val bmp = remember(block) {
                                                BitmapFactory.decodeByteArray(block.imageBytes, 0, block.imageBytes.size)
                                            }
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = block.name,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 220.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }

                                        WordBlock.PageBreakBlock -> {
                                            Text(
                                                text = "— নতুন পৃষ্ঠা —",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MutedInk,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                if (doc.blocks.size > 40) {
                                    Text(
                                        text = "... আরও ${BengaliFormatters.toBengaliDigits(doc.blocks.size - 40)}টি অংশ মূল পিডিএফে অন্তর্ভুক্ত হবে",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MutedInk
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = CardSurface,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isConverting) {
                                LinearProgressIndicator(
                                    progress = {
                                        if (uiState.progressTotal > 0) {
                                            uiState.progressCurrent.toFloat() / uiState.progressTotal
                                        } else 0.5f
                                    },
                                    color = RoyalBlue,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "পিডিএফ তৈরি হচ্ছে (${BengaliFormatters.toBengaliDigits(uiState.progressCurrent)} / ${BengaliFormatters.toBengaliDigits(uiState.progressTotal)})...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk
                                )
                            }
                            Button(
                                onClick = onConvertToPdf,
                                enabled = !uiState.isConverting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("word_pdf_convert_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isConverting) "রূপান্তর চলছে..." else "পিডিএফে রূপান্তর ও সংরক্ষণ করুন",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
