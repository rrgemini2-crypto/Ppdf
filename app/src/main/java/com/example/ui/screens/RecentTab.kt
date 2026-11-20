package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.PdfDocumentEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LightBlueHeader
import com.example.ui.components.PdfDocumentCard

@Composable
fun RecentTab(
    documents: List<PdfDocumentEntity>,
    onOpenPdf: (PdfDocumentEntity) -> Unit,
    onRenamePdf: (PdfDocumentEntity) -> Unit,
    onToggleFavorite: (PdfDocumentEntity) -> Unit,
    onSharePdf: (PdfDocumentEntity) -> Unit,
    onViewInfo: (PdfDocumentEntity) -> Unit,
    onDeletePdf: (PdfDocumentEntity) -> Unit,
    onCreateNewClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "সাম্প্রতিক",
            subtitle = "সর্বশেষ তৈরি এবং খোলা পিডিএফ ফাইলসমূহ"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            if (documents.isEmpty()) {
                EmptyStateView(
                    title = "সাম্প্রতিক কোনো ফাইল নেই",
                    subtitle = "আপনি নতুন পিডিএফ তৈরি করলে বা কোনো ফাইল খুললে এখানে স্বয়ংক্রিয়ভাবে দেখাবে।",
                    actionLabel = "নতুন পিডিএফ তৈরি করুন",
                    onActionClick = onCreateNewClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        PdfDocumentCard(
                            document = doc,
                            onOpen = { onOpenPdf(doc) },
                            onRename = { onRenamePdf(doc) },
                            onToggleFavorite = { onToggleFavorite(doc) },
                            onShare = { onSharePdf(doc) },
                            onViewInfo = { onViewInfo(doc) },
                            onDelete = { onDeletePdf(doc) }
                        )
                    }
                }
            }
        }
    }
}
