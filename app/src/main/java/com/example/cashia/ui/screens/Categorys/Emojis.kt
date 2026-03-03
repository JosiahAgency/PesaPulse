package com.example.cashia.ui.screens.Categorys

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val EMOJI_SUGGESTIONS = listOf(
    "🏠", "🍔", "🚗", "💡", "📱", "🎬", "💊", "🏦", "💼", "💸",
    "🏧", "📥", "📚", "🏛️", "🛡️", "📈", "📦", "🎁", "🐾", "⚽",
    "✈️", "🌍", "💄", "👗", "🎓", "🔧", "🌾", "⛽", "🍺", "☕",
    "🏋️", "🎮", "📷", "🏥", "🌿", "🎵", "🛒", "🎂", "💍", "🏡",
    "💰", "🧾", "📊", "📉", "🪙", "💳", "🏢", "📑", "🖨️", "🗂️",
    "🍕", "🍟", "🌮", "🍣", "🍩", "🥗", "🍜", "🥤", "🍷", "🥞",
    "🚕", "🚌", "🚆", "🚲", "🛵", "🚚", "🚢", "🚁", "🛴", "🚤",
    "💻", "🖥️", "⌚", "🖱️", "⌨️", "🧑‍💻", "🛜", "🔌", "🔋", "📡",
    "🩺", "🧠", "🦷", "🧴", "🚿", "🧘", "🥼", "🧃", "🥦", "🍎",
    "🎤", "🎧", "🎹", "🎻", "🎨", "🖌️", "📺", "🎭", "🎪", "🎯",
    "🛏️", "🛋️", "🚪", "🪑", "🧹", "🧺", "🧯", "🕯️", "🧊", "🪟",
    "🌳", "🌸", "🌞", "🌧️", "⛄", "🔥", "⚡", "🌊", "🏔️", "🌙"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPicker(
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(40.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.heightIn(max = 240.dp)
    ) {
        items(
            EMOJI_SUGGESTIONS,
            key = { it }
        ) { emoji ->
            val isSelected = emoji == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable { onSelect(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }
        }
    }
}