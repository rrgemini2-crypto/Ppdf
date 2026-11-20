package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.WorkspacePage
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LightBlueHeader
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
import kotlin.math.roundToInt

@Composable
fun WorkspaceScreen(
    pages: List<WorkspacePage>,
    onBack: () -> Unit,
    onAddFromGallery: () -> Unit,
    onAddFromCamera: () -> Unit,
    onAddFromScanner: () -> Unit,
    onEditPage: (String) -> Unit,
    onRotatePage: (String) -> Unit,
    onDeletePage: (String) -> Unit,
    onMovePage: (Int, Int) -> Unit,
    onProceedToSettings: () -> Unit
) {
    BackHandler(onBack = onBack)

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightBlueHeader(
            title = "ছবি সাজানো ও প্রিভিউ",
            subtitle = "নির্বাচিত ছবি: ${BengaliFormatters.toBengaliDigits(pages.size)}টি • চেপে ধরে টেনে ক্রম বদলান",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            // Action bar to add more images from gallery / camera / scanner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddFromGallery,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("workspace_add_gallery"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("গ্যালারি", color = RoyalBlue, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onAddFromCamera,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("workspace_add_camera"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ক্যামেরা", color = RoyalBlue, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onAddFromScanner,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("workspace_add_scan"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue)
                ) {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("স্ক্যান", color = RoyalBlue, maxLines = 1)
                }
            }

            if (pages.isEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyStateView(
                        title = "কোনো ছবি যুক্ত করা হয়নি",
                        subtitle = "পিডিএফ তৈরি করতে গ্যালারি থেকে ছবি বাছুন অথবা ক্যামেরা দিয়ে দলিল স্ক্যান করুন।",
                        actionLabel = "গ্যালারি থেকে ছবি বাছুন",
                        onActionClick = onAddFromGallery
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                        val isDragging = draggedIndex == index
                        val itemOffsetY = if (isDragging) dragOffsetY.roundToInt() else 0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .offset { IntOffset(0, itemOffsetY) }
                                .pointerInput(pages.size, index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val rowHeightPx = 112.dp.toPx()
                                            val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                            if (dragOffsetY > rowHeightPx * 0.65f && currentIdx < pages.lastIndex) {
                                                onMovePage(currentIdx, currentIdx + 1)
                                                draggedIndex = currentIdx + 1
                                                dragOffsetY -= rowHeightPx
                                            } else if (dragOffsetY < -rowHeightPx * 0.65f && currentIdx > 0) {
                                                onMovePage(currentIdx, currentIdx - 1)
                                                draggedIndex = currentIdx - 1
                                                dragOffsetY += rowHeightPx
                                            }
                                        }
                                    )
                                }
                                .clickable { onEditPage(page.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDragging) LightBlueTint else CardSurface
                            ),
                            border = BorderStroke(
                                width = if (isDragging) 2.dp else 1.dp,
                                color = if (isDragging) RoyalBlue else SubtleBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DragIndicator,
                                    contentDescription = "টেনে সাজান",
                                    tint = MutedInk,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 82.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(LightBlueTint)
                                        .border(1.dp, SubtleBorder, RoundedCornerShape(10.dp))
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(File(page.previewPath)),
                                        contentDescription = BengaliFormatters.formatOrdinalPage(index + 1),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        color = RoyalBlue,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = BengaliFormatters.toBengaliDigits(index + 1),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WhiteBackground,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = BengaliFormatters.formatOrdinalPage(index + 1),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ফিল্টার: ${page.editParams.filterType.titleBn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedInk
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = { onEditPage(page.id) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "সম্পাদনা", tint = RoyalBlue)
                                        }
                                        IconButton(
                                            onClick = { onRotatePage(page.id) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Filled.RotateRight, contentDescription = "ঘোরানো", tint = RoyalBlue)
                                        }
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
                                            enabled = index < pages.lastIndex,
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                Icons.Filled.ArrowDownward,
                                                contentDescription = "নিচে নিন",
                                                tint = if (index < pages.lastIndex) DeepInk else SubtleBorder
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeletePage(page.id) },
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
            }

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
                        onClick = onProceedToSettings,
                        enabled = pages.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("workspace_next_settings_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "পরবর্তী: পিডিএফ সেটিংস (${BengaliFormatters.toBengaliDigits(pages.size)} পৃষ্ঠা)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
