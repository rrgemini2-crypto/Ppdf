package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LightBlueHeader
import com.example.ui.theme.LightBlueHeaderBorder
import com.example.ui.theme.MutedInk

@Composable
fun LightBlueHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = LightBlueHeader,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .border(1.dp, LightBlueHeaderBorder, RoundedCornerShape(12.dp))
                                .testTag("header_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "পিছনে যান",
                                tint = DeepInk
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = DeepInk,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = actions
                )
            }
            if (bottomContent != null) {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    bottomContent()
                }
            }
        }
    }
}
