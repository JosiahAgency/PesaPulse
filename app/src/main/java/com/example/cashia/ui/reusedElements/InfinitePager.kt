package com.example.cashia.ui.reusedElements

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable

/**
 * Shared geometry for the app's "endless" date pagers (the dashboard week strip and the
 * calendar month grid).
 *
 * Compose has no truly unbounded pager, so both emulate one by centring a very large page
 * window on today and mapping `page - ANCHOR_PAGE` to a period offset. The window is wide
 * enough (~500,000 periods either way) that it outlasts `LocalDate`'s useful range, unlike
 * the 1,000-page/page-500 window this replaced — that one silently stopped scrolling after
 * a few hundred swipes.
 *
 * Both calendars previously carried their own copy of these magic numbers.
 */
object InfinitePager {
    const val PAGE_COUNT = 1_000_001
    const val ANCHOR_PAGE = PAGE_COUNT / 2

    /** Page showing the period [offset] steps from the anchor, clamped to the window. */
    fun pageFor(offset: Int): Int = (ANCHOR_PAGE + offset).coerceIn(0, PAGE_COUNT - 1)

    /** Period offset from the anchor that [page] represents. */
    fun offsetOf(page: Int): Int = page - ANCHOR_PAGE
}

/** A [PagerState] pre-centred on [InfinitePager.ANCHOR_PAGE]. */
@Composable
fun rememberInfinitePagerState(): PagerState = rememberPagerState(
    initialPage = InfinitePager.ANCHOR_PAGE,
    pageCount = { InfinitePager.PAGE_COUNT }
)
