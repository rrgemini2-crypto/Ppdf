package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.PdfDocumentEntity
import com.example.ui.OrganizerUiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LightBlueHeader
import com.example.ui.components.SelectPdfDocumentDialog
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters
import java.io.File

@Composable
fun PageOrganizerScreen(
    uiState: OrganizerUiState,
    appDocuments: List<PdfDocumentEntity>,
    onPickFromMobile: () -> Unit,
    onSelectAppDocument: (PdfDocumentEntity) -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onRotatePage: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onSaveOrganized: (saveAsCopy: Boolean) -> Unit,
    onOpenSavedPdf: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedDocumentId) {
        uiState.savedDocumentId?.let { id ->
            onOpenSavedPdf(id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "পৃষ্ঠা সাজানো",
            subtitle = if (uiState.pages.isNotEmpty()) {
                "${uiState.sourceName} • ${BengaliFormatters.formatPageCount(uiState.pages.size)}"
            } else {
                "পিডিএফের পৃষ্ঠা আগে-পরে সাজান, ঘোরান বা বাদ দিন"
            },
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .navigationBarsPadding()
        ) {
            when {
                uiState.isLoading || uiState.isSaving -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RoyalBlue)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (uiState.isSaving) "সাজানো পিডিএফ সংরক্ষণ হচ্ছে..." else "পৃষ্ঠাগুলো লোড হচ্ছে...",
                                color = MutedInk
                            )
                        }
                    }
                }

                uiState.errorBn != null -> {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView(
                            title = "সমস্যা হয়েছে",
                            subtitle = uiState.errorBn,
                            actionLabel = "অন্য ফাইল বাছুন",
                            onActionClick = { showPicker = true }
                        )
                    }
                }

                uiState.pages.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView(
                            title = "পিডিএফ ফাইল নির্বাচন করুন",
                            subtitle = "পৃষ্ঠার ক্রম পরিবর্তন, ঘোরানো অথবা অপ্রয়োজনীয় পৃষ্ঠা বাদ দিতে একটি পিডিএফ বাছুন।",
                            actionLabel = "পিডিএফ বাছুন",
                            onActionClick = { showPicker = true }
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(uiState.pages) { index, item ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                border = BorderStroke(1.dp, SubtleBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 60.dp, height = 80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LightBlueTint)
                                            .border(1.dp, SubtleBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(File(item.imagePath)),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(item.rotationDegrees.toFloat()),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = BengaliFormatters.formatOrdinalPage(index + 1),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DeepInk,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "মূল পৃষ্ঠা: ${BengaliFormatters.toBengaliDigits(item.originalPageNumber)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedInk
                                        )
                                        Row {
                                            IconButton(
                                                onClick = { onMovePage(index, index - 1) },
                                                enabled = index > 0,
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    Icons.Filled.ArrowUpward,
                                                    contentDescription = "উপরে নিন",
                                                    tint = if (index > 0) DeepInk else SubtleBorder
                                                )
                                            }
                                            IconButton(
                                                onClick = { onMovePage(index, index + 1) },
                                                enabled = index < uiState.pages.lastIndex,
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    Icons.Filled.ArrowDownward,
                                                    contentDescription = "নিচে নিন",
                                                    tint = if (index < uiState.pages.lastIndex) DeepInk else SubtleBorder
                                                )
                                            }
                                            IconButton(
                                                onClick = { onRotatePage(index) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(Icons.Filled.RotateRight, contentDescription = "ঘোরানো", tint = RoyalBlue)
                                            }
                                            IconButton(
                                                onClick = { onDeletePage(index) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "বাদ দিন", tint = ErrorCrimson)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.sourceDocumentId != null) {
                            OutlinedButton(
                                onClick = { onSaveOrganized(true) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("নতুন কপি সেভ")
                            }
                            Button(
                                onClick = { onSaveOrganized(false) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("আপডেট করুন")
                            }
                        } else {
                            Button(
                                onClick = { onSaveOrganized(true) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("নতুন কপি হিসেবে সেভ করুন")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        SelectPdfDocumentDialog(
            title = "পৃষ্ঠা সাজানোর পিডিএফ বাছুন",
            documents = appDocuments,
            allowExternalPick = true,
            onPickExternal = onPickFromMobile,
            onSelectDocument = onSelectAppDocument,
            onDismiss = { showPicker = false }
        )
    }
}
