package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ImageFilterType
import com.example.data.model.NormPoint
import com.example.data.model.NormRect
import com.example.data.model.PageEditParams
import com.example.data.model.WorkspacePage
import com.example.engine.DocumentCornerDetector
import com.example.engine.ImageFilterProcessor
import com.example.ui.components.LightBlueHeader
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LightBlueTint
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftSurface
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.WhiteBackground
import com.example.util.BengaliFormatters
import com.example.util.FileActionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot

@Composable
fun ImageEditorScreen(
    page: WorkspacePage,
    initialTabIndex: Int = 0, // 0 = Filters, 1 = Crop/Corners, 2 = Tune/Rotate
    showSaveImageToGalleryButton: Boolean = false,
    onSaveParams: (PageEditParams) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editParams by remember(page.id) { mutableStateOf(page.editParams) }
    var selectedTab by remember(initialTabIndex) { mutableIntStateOf(initialTabIndex.coerceIn(0, 2)) }

    // For tab 1 (Crop / Corners): edit raw oriented bitmap so user sees full corners to drag
    val rawBitmap by produceState<Bitmap?>(initialValue = null, key1 = page.originalPath) {
        value = withContext(Dispatchers.IO) {
            ImageFilterProcessor.decodeOrientedBitmap(page.originalPath, 1080)
        }
    }

    // Live processed bitmap preview
    val previewBitmap by produceState<Bitmap?>(initialValue = null, key1 = page.originalPath, key2 = editParams) {
        value = withContext(Dispatchers.IO) {
            ImageFilterProcessor.processPageBitmap(page.originalPath, editParams, 1080)
        }
    }

    // Pre-generate 10 filter thumbnails for instant horizontal picker
    val filterThumbnails by produceState<Map<ImageFilterType, Bitmap>>(
        initialValue = emptyMap(),
        key1 = page.originalPath
    ) {
        value = withContext(Dispatchers.IO) {
            val base = ImageFilterProcessor.decodeOrientedBitmap(page.originalPath, 180) ?: return@withContext emptyMap()
            val map = ImageFilterType.entries.associateWith { type ->
                ImageFilterProcessor.renderThumbnailForFilter(base, type)
            }
            base.recycle()
            map
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
    ) {
        LightBlueHeader(
            title = "ছবি ও স্ক্যান সম্পাদনা",
            subtitle = "ফিল্টার: ${editParams.filterType.titleBn}",
            onBackClick = onBack,
            actions = {
                if (showSaveImageToGalleryButton) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val full = withContext(Dispatchers.IO) {
                                    ImageFilterProcessor.processPageBitmap(page.originalPath, editParams, 2200)
                                }
                                if (full != null) {
                                    val res = withContext(Dispatchers.IO) {
                                        FileActionsHelper.saveBitmapToGallery(
                                            context,
                                            full,
                                            "সম্পাদিত_${System.currentTimeMillis()}"
                                        )
                                    }
                                    full.recycle()
                                    res.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "ছবিটি গ্যালারিতে সংরক্ষিত হয়েছে।", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = {
                                            Toast.makeText(context, "ছবি সংরক্ষণ করা যায়নি।", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.SaveAlt, contentDescription = "ছবি সংরক্ষণ", tint = RoyalBlue)
                    }
                }
                IconButton(
                    onClick = { editParams = PageEditParams() },
                    modifier = Modifier.testTag("editor_reset_button")
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "মূল ছবিতে ফিরে যান", tint = DeepInk)
                }
            }
        )

        // Preview Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SoftSurface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTab == 1 && rawBitmap != null) {
                CornerAndCropCanvas(
                    bitmap = rawBitmap!!,
                    corners = editParams.corners,
                    onCornersChange = { newCorners ->
                        editParams = editParams.copy(
                            corners = newCorners,
                            usePerspectiveWarp = true
                        )
                    }
                )
            } else if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = "সম্পাদিত প্রিভিউ",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Controls Panel
        Surface(
            color = WhiteBackground,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, SubtleBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = WhiteBackground,
                    contentColor = RoyalBlue
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("ফিল্টার") },
                        icon = { Icon(Icons.Filled.FilterVintage, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("কাটছাঁট ও কোণা") },
                        icon = { Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("আলো ও ঘোরানো") },
                        icon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(ImageFilterType.entries) { filter ->
                                val isSelected = editParams.filterType == filter
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(78.dp)
                                        .clickable {
                                            editParams = editParams.copy(filterType = filter)
                                        }
                                        .testTag("filter_item_${filter.name.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 70.dp, height = 76.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) RoyalBlue else SubtleBorder,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .background(LightBlueTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val thumb = filterThumbnails[filter]
                                        if (thumb != null) {
                                            Image(
                                                bitmap = thumb.asImageBitmap(),
                                                contentDescription = filter.titleBn,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = filter.titleBn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) RoyalBlue else DeepInk,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "চারটি কোণা টেনে কাগজের সীমানা ঠিক করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("সোজা করুন", style = MaterialTheme.typography.labelMedium, color = DeepInk)
                                    Spacer(Modifier.width(6.dp))
                                    Switch(
                                        checked = editParams.usePerspectiveWarp,
                                        onCheckedChange = {
                                            editParams = editParams.copy(usePerspectiveWarp = it)
                                        }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val bmp = rawBitmap
                                        if (bmp != null) {
                                            val detected = DocumentCornerDetector.detectFromBitmap(bmp)
                                            editParams = editParams.copy(
                                                corners = detected.corners,
                                                usePerspectiveWarp = true
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("স্বয়ংক্রিয় কোণা")
                                }
                                OutlinedButton(
                                    onClick = {
                                        editParams = editParams.copy(
                                            corners = PageEditParams.defaultCorners(),
                                            cropRect = NormRect(),
                                            usePerspectiveWarp = false
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("সম্পূর্ণ ছবি রাখুন")
                                }
                            }
                        }
                    }

                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "উজ্জ্বলতা: ${BengaliFormatters.toBengaliDigits(editParams.brightness.toInt())}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepInk
                                )
                                Text(
                                    text = "কনট্রাস্ট: ${BengaliFormatters.toBengaliDigits(((editParams.contrast - 1f) * 100).toInt())}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepInk
                                )
                            }
                            Slider(
                                value = editParams.brightness,
                                onValueChange = { editParams = editParams.copy(brightness = it) },
                                valueRange = -80f..80f,
                                colors = SliderDefaults.colors(thumbColor = RoyalBlue, activeTrackColor = RoyalBlue)
                            )
                            Slider(
                                value = editParams.contrast,
                                onValueChange = { editParams = editParams.copy(contrast = it) },
                                valueRange = 0.6f..1.8f,
                                colors = SliderDefaults.colors(thumbColor = RoyalBlue, activeTrackColor = RoyalBlue)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        editParams = editParams.copy(
                                            rotationDegrees = (editParams.rotationDegrees + 90) % 360
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("৯০° ঘোরানো")
                                }
                                OutlinedButton(
                                    onClick = { editParams = PageEditParams() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("মূল ছবিতে যান")
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = { onSaveParams(editParams) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("editor_save_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("পরিবর্তন সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CornerAndCropCanvas(
    bitmap: Bitmap,
    corners: List<NormPoint>,
    onCornersChange: (List<NormPoint>) -> Unit
) {
    val density = LocalDensity.current
    var activeCornerIdx by remember { mutableIntStateOf(-1) }
    var currentCorners by remember(corners) { mutableStateOf(corners) }

    LaunchedEffect(corners) {
        currentCorners = corners
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val imgRatio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
        val boxRatio = maxW / maxH.coerceAtLeast(1f)
        val drawW = if (imgRatio > boxRatio) maxW else maxH * imgRatio
        val drawH = if (imgRatio > boxRatio) maxW / imgRatio else maxH

        val widthDp = with(density) { drawW.toDp() }
        val heightDp = with(density) { drawH.toDp() }

        Box(
            modifier = Modifier
                .size(widthDp, heightDp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "কোণা সম্পাদনা",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(drawW, drawH) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val nx = offset.x / drawW
                                val ny = offset.y / drawH
                                var bestIdx = -1
                                var bestDist = 0.16f
                                currentCorners.forEachIndexed { idx, pt ->
                                    val d = hypot(pt.x - nx, pt.y - ny)
                                    if (d < bestDist) {
                                        bestDist = d
                                        bestIdx = idx
                                    }
                                }
                                activeCornerIdx = bestIdx
                            },
                            onDragEnd = {
                                if (activeCornerIdx >= 0) {
                                    onCornersChange(currentCorners)
                                }
                                activeCornerIdx = -1
                            },
                            onDragCancel = {
                                activeCornerIdx = -1
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val idx = activeCornerIdx
                                if (idx in 0..3) {
                                    val prev = currentCorners[idx]
                                    val updated = NormPoint(
                                        x = (prev.x + dragAmount.x / drawW).coerceIn(0.01f, 0.99f),
                                        y = (prev.y + dragAmount.y / drawH).coerceIn(0.01f, 0.99f)
                                    )
                                    val nextList = currentCorners.toMutableList()
                                    nextList[idx] = updated
                                    currentCorners = nextList
                                }
                            }
                        )
                    }
            ) {
                if (currentCorners.size == 4) {
                    val pts = currentCorners.map { Offset(it.x * size.width, it.y * size.height) }
                    val path = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        lineTo(pts[1].x, pts[1].y)
                        lineTo(pts[2].x, pts[2].y)
                        lineTo(pts[3].x, pts[3].y)
                        close()
                    }
                    drawPath(path = path, color = RoyalBlue.copy(alpha = 0.18f))
                    drawPath(
                        path = path,
                        color = RoyalBlue,
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    pts.forEachIndexed { i, offset ->
                        val radius = if (i == activeCornerIdx) 28f else 22f
                        drawCircle(color = RoyalBlue, radius = radius, center = offset)
                        drawCircle(color = Color.White, radius = radius * 0.52f, center = offset)
                    }
                }
            }
        }
    }
}
