package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.DeepInk
import com.example.ui.theme.MutedInk
import com.example.ui.theme.RoyalBlue

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_documents_1795149286820),
            contentDescription = title,
            modifier = Modifier
                .widthIn(max = 240.dp)
                .height(175.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = DeepInk,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedInk,
            textAlign = TextAlign.Center
        )
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_state_action_button")
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
