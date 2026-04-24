package com.example.cashia.utility

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Emoji catalogue grouped by theme ─────────────────────────────────────────

data class EmojiGroup(val label: String, val icon: String, val emojis: List<String>)

val EMOJI_GROUPS = listOf(
    EmojiGroup(
        "Finance", "💰", listOf(
            "💰", "💳", "💵", "💴", "💶", "💷", "🏦", "💸", "🪙", "💹",
            "📈", "📉", "📊", "🧾", "🏧", "💼", "📑", "🗂️", "📋", "🔐"
        )
    ),
    EmojiGroup(
        "Food", "🍔", listOf(
            "🍔", "🍕", "🍟", "🌮", "🍣", "🍜", "🥗", "🍩", "🥞", "☕",
            "🍷", "🥤", "🍺", "🧃", "🍎", "🥦", "🌽", "🥕", "🛒", "🍫"
        )
    ),
    EmojiGroup(
        "Transport", "🚗", listOf(
            "🚗", "🚕", "🚌", "🚆", "🚲", "🛵", "🚚", "🚢", "🚁", "✈️",
            "🛴", "🚤", "⛽", "🅿️", "🗺️", "🧭", "🛣️", "🚦", "🏎️", "🚐"
        )
    ),
    EmojiGroup(
        "Home", "🏠", listOf(
            "🏠", "🏡", "🏢", "🛋️", "🛏️", "🚪", "🪑", "🧹", "🧺", "🧯",
            "🕯️", "🧊", "🪟", "🔧", "🔨", "🪠", "🛁", "🚿", "🪞", "🧴"
        )
    ),
    EmojiGroup(
        "Health", "💊", listOf(
            "💊", "🩺", "🏥", "🧠", "🦷", "👁️", "🩹", "🩻", "🧬", "💉",
            "🧘", "🏋️", "🥼", "🚑", "🌡️", "🫀", "🫁", "🩸", "🏃", "🧪"
        )
    ),
    EmojiGroup(
        "Shopping", "🛍️", listOf(
            "🛍️", "👗", "👟", "👜", "💍", "⌚", "📦", "🎁", "🛒", "💄",
            "👒", "🧣", "🧥", "👔", "🩴", "🎒", "🧤", "🕶️", "💎", "🪮"
        )
    ),
    EmojiGroup(
        "Business", "📊", listOf(
            "📊", "📈", "📉", "💼", "🏪", "🧑‍💼", "📦", "🧾", "🗂️", "💰"
        )
    ),
    EmojiGroup(
        "Tech", "💻", listOf(
            "💻", "🖥️", "📱", "⌚", "🖱️", "⌨️", "🔌", "🔋", "📡", "🛜",
            "🖨️", "📷", "🎮", "🕹️", "📺", "📻", "🎧", "📸", "🔭", "🧑‍💻"
        )
    ),
    EmojiGroup(
        "Education", "📚", listOf(
            "📚", "🎓", "📝", "✏️", "🖊️", "📖", "🏫", "🔬", "📐", "📏",
            "🗒️", "📌", "📎", "🖇️", "🗃️", "📰", "🔢", "🧮", "🎒", "📓"
        )
    ),
    EmojiGroup(
        "Fun", "🎬", listOf(
            "🎬", "🎵", "🎤", "🎹", "🎻", "🎨", "🖌️", "🎭", "🎪", "🎯",
            "⚽", "🏀", "🎸", "🧩", "♟️", "🎲", "🎰", "🎳", "🏆", "🥇"
        )
    ),
    EmojiGroup(
        "Family & Support", "👨‍👩‍👧", listOf(
            "👨‍👩‍👧", "👶", "👵", "👴", "🙏", "🤝", "💞", "🏠", "🫂", "❤️"
        )
    ),
    EmojiGroup(
        "Entertainment", "🎉", listOf(
            "🎉", "🎬", "🎵", "⚽", "🏀", "🎮", "🎤", "🎧", "🍿", "🎲"
        )
    ),

    EmojiGroup(
        "Personal", "🧍", listOf(
            "🧍", "💇", "💅", "🧴", "🪞", "🛁", "🧼", "👕", "🧢", "🕶️"
        )
    ),
    EmojiGroup(
        "Government", "🏛️", listOf(
            "🏛️", "🗳️", "📜", "⚖️", "🔏", "🪪", "🛂", "🏗️", "🌐", "🤝",
            "📢", "🔔", "🪖", "🚔", "🚒", "🚁", "🏴", "🎌", "🇰🇪", "🗺️"
        )
    ),
    EmojiGroup(
        "Bills & Utilities", "💡", listOf(
            "💡", "🔌", "📶", "📡", "💧", "🚰", "🔥", "🏠", "📺", "🧾"
        )
    ),
)

val ALL_EMOJIS = EMOJI_GROUPS.flatMap { it.emojis }.distinct()

private val recentlyUsed = mutableListOf<String>()

fun recordEmojiUse(emoji: String) {
    recentlyUsed.remove(emoji)
    recentlyUsed.add(0, emoji)
    if (recentlyUsed.size > 16) recentlyUsed.removeAt(recentlyUsed.lastIndex)
}

@Composable
fun EmojiPicker(
    selected: String,
    onSelect: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Build tab list: Recent (if any) + all groups
    val tabs = remember(recentlyUsed.size) {
        buildList {
            if (recentlyUsed.isNotEmpty()) add(EmojiGroup("Recent", "🕐", recentlyUsed.toList()))
            addAll(EMOJI_GROUPS)
        }
    }
    if (recentlyUsed.isNotEmpty()) 1 else 0

    // Search filter
    val searchResults = remember(query) {
        if (query.isBlank()) emptyList()
        else ALL_EMOJIS.filter { emoji ->
            // Simple approach: match against group labels as a hint
            val groupMatch = EMOJI_GROUPS.any { g ->
                g.emojis.contains(emoji) &&
                        g.label.contains(query, ignoreCase = true)
            }
            groupMatch || emoji.contains(query)
        }.take(40)
    }

    val showSearch = query.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Search bar ────────────────────────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text(
                    "Search emojis…",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search, null,
                    modifier = Modifier.size(18.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            trailingIcon = {
                AnimatedVisibility(query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.12f),
                focusedBorderColor = colorScheme.primary.copy(alpha = 0.5f),
                unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = colorScheme.primary.copy(alpha = 0.03f)
            )
        )

        if (showSearch) {
            // ── Search results grid ───────────────────────────────────────
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No results for \"$query\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                EmojiGrid(
                    emojis = searchResults,
                    selected = selected,
                    onSelect = { emoji ->
                        recordEmojiUse(emoji)
                        onSelect(emoji)
                        query = ""
                    }
                )
            }
        } else {
            // ── Category tabs ─────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex),
                containerColor = colorScheme.surface.copy(alpha = 0f),
                contentColor = colorScheme.primary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    val idx = selectedTab.coerceAtMost(tabPositions.lastIndex)
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                        height = 2.dp,
                        color = colorScheme.primary
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { i, group ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Text(group.icon, fontSize = 16.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                group.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedTab == i) colorScheme.primary
                                else colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            val currentGroup = tabs.getOrNull(selectedTab)
            if (currentGroup != null) {
                EmojiGrid(
                    emojis = currentGroup.emojis,
                    selected = selected,
                    onSelect = { emoji ->
                        recordEmojiUse(emoji)
                        onSelect(emoji)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmojiGrid(
    emojis: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyVerticalGrid(
        columns = GridCells.Adaptive(40.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier.heightIn(max = 200.dp)
    ) {
        items(emojis, key = { it }) { emoji ->
            val isSelected = emoji == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) colorScheme.primary.copy(alpha = 0.18f)
                        else colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .then(
                        if (isSelected) Modifier.then(
                            Modifier.background(
                                colorScheme.primary.copy(alpha = 0.0f),
                                RoundedCornerShape(10.dp)
                            )
                        ) else Modifier
                    )
                    .clickable { onSelect(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp, textAlign = TextAlign.Center)
                if (isSelected) {
                    // Small checkmark badge
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 7.sp, color = colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
