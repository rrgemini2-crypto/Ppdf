package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.PdfDocumentEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LightBlueHeader
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftSurface
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PdfViewerScreen(
    document: PdfDocumentEntity,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onViewInfo: () -> Unit,
    onOrganizePages: () -> Unit
) {
    BackHandler(onBack = onBack)

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val renderMutex = remember { Mutex() }

    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var loadErrorBn by remember { mutableStateOf<String?>(null) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var showJumpDialog by remember { mutableStateOf(false) }

    DisposableEffect(document.filePath) {
        val file = File(document.filePath)
        if (!file.exists()) {
            loadErrorBn = "পিডিএফ ফাইলটি ডিভাইসে পাওয়া যায়নি।"
        } else {
            try {
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(descriptor)
                pfd = descriptor
                renderer = pdfRenderer
                pageCount = pdfRenderer.pageCount
                if (pdfRenderer.pageCount <= 0) {
                    loadErrorBn = "এই পিডিএফ ফাইলটিতে কোনো পৃষ্ঠা নেই।"
                }
            } catch (_: Exception) {
                loadErrorBn = "পিডিএফ ফাইলটি ক্ষতিগ্রস্ত অথবা খোলা যাচ্ছে না।"
            }
        }
        onDispose {
            try {
                renderer?.close()
            } catch (_: Exception) {
            }
            try {
                pfd?.close()
            } catch (_: Exception) {
            }
            renderer = null
            pfd = null
        }
    }

    val currentPageIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSurface)
    ) {
        LightBlueHeader(
            title = document.name,
            subtitle = if (pageCount > 0) {
                "পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(currentPageIndex + 1)} / ${BengaliFormatters.toBengaliDigits(pageCount)} • ${BengaliFormatters.formatFileSize(document.sizeBytes)}"
            } else {
                BengaliFormatters.formatFileSize(document.sizeBytes)
            },
            onBackClick = onBack,
            actions = {
                IconButton(onClick = onOrganizePages, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Filled.Reorder, contentDescription = "পৃষ্ঠা সাজানো", tint = DeepInk)
                }
                IconButton(onClick = onPrint, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Filled.Print, contentDescription = "প্রিন্ট", tint = DeepInk)
                }
                IconButton(onClick = onViewInfo, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Filled.Info, contentDescription = "ফাইলের তথ্য", tint = DeepInk)
                }
                IconButton(onClick = onShare, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Filled.Share, contentDescription = "শেয়ার করুন", tint = RoyalBlue)
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale > 1.1f) {
                                zoomScale = 1f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            } else {
                                zoomScale = 2f
                            }
                        }
                    )
                }
                .pointerInput(zoomScale) {
                    if (zoomScale > 1.02f) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                            zoomScale = newScale
                            if (newScale > 1.02f) {
                                panOffsetX = (panOffsetX + pan.x).coerceIn(-600f, 600f)
                                panOffsetY = (panOffsetY + pan.y).coerceIn(-600f, 600f)
                            } else {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        }
                    }
                }
        ) {
            if (loadErrorBn != null) {
                EmptyStateView(
                    title = "ফাইল খোলা যায়নি",
                    subtitle = loadErrorBn!!,
                    actionLabel = "ফিরে যান",
                    onActionClick = onBack
                )
            } else if (renderer != null && pageCount > 0) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffsetX
                            translationY = panOffsetY
                        },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(pageCount, key = { it }) { pageIdx ->
                        PdfPageBitmapItem(
                            renderer = renderer!!,
                            pageIndex = pageIdx,
                            mutex = renderMutex
                        )
                    }
                }
            }
        }

        if (pageCount > 0 && loadErrorBn == null) {
            Surface(
                color = WhiteBackground,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val prev = (currentPageIndex - 1).coerceAtLeast(0)
                                scope.launch { listState.animateScrollToItem(prev) }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "আগের পৃষ্ঠা",
                                tint = if (currentPageIndex > 0) RoyalBlue else SubtleBorder
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LightBlueTint,
                            modifier = Modifier
                                .clickable { showJumpDialog = true }
                                .testTag("pdf_viewer_page_jump")
                        ) {
                            Text(
                                text = "পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(currentPageIndex + 1)} / ${BengaliFormatters.toBengaliDigits(pageCount)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepInk,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val next = (currentPageIndex + 1).coerceAtMost(pageCount - 1)
                                scope.launch { listState.animateScrollToItem(next) }
                            },
                            enabled = currentPageIndex < pageCount - 1,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "পরের পৃষ্ঠা",
                                tint = if (currentPageIndex < pageCount - 1) RoyalBlue else SubtleBorder
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                zoomScale = (zoomScale - 0.4f).coerceAtLeast(1f)
                                if (zoomScale <= 1.02f) {
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                            },
                            enabled = zoomScale > 1.05f,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.ZoomOut, contentDescription = "ছোট করুন", tint = DeepInk)
                        }
                        Text(
                            text = "${BengaliFormatters.toBengaliDigits((zoomScale * 100).roundToInt())}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedInk
                        )
                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.4f).coerceAtMost(3.5f) },
                            enabled = zoomScale < 3.4f,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = "বড় করুন", tint = RoyalBlue)
                        }
                    }
                }
            }
        }
    }

    if (showJumpDialog && pageCount > 0) {
        var pageInput by remember { mutableStateOf((currentPageIndex + 1).toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            containerColor = WhiteBackground,
            title = { Text("নির্দিষ্ট পৃষ্ঠায় যান", color = DeepInk) },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("পৃষ্ঠা নম্বর (১ - ${BengaliFormatters.toBengaliDigits(pageCount)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = (pageInput.toIntOrNull() ?: 1).coerceIn(1, pageCount) - 1
                        showJumpDialog = false
                        scope.launch { listState.animateScrollToItem(target) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("যান")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("বাতিল", color = MutedInk)
                }
            }
        )
    }
}

@Composable
private fun PdfPageBitmapItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    mutex: Mutex
) {
    val pageState by produceState<Pair<Bitmap, Float>?>(initialValue = null, key1 = renderer, key2 = pageIndex) {
        value = withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    renderer.openPage(pageIndex).use { page ->
                        val ratio = page.width.toFloat() / max(1, page.height)
                        val renderW = 1080
                        val renderH = max(200, (renderW / ratio).roundToInt())
                        val bmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp to ratio
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WhiteBackground,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val pair = pageState
            if (pair != null) {
                Image(
                    bitmap = pair.first.asImageBitmap(),
                    contentDescription = "পৃষ্ঠা ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(pair.second)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RoyalBlue)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBlueTint)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = BengaliFormatters.formatOrdinalPage(pageIndex + 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedInk
                )
            }
        }
    }
}
