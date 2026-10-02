package com.example.cashia.utility

import android.util.Log
import androidx.room.withTransaction
import com.example.cashia.AppDatabase
import com.example.cashia.daos.UnmatchedSmsDao
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.utility.ParserLearningService.clusterByStructure
import com.example.cashia.utility.ParserLearningService.reprocessOutdated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ParserLearning"

// Precompiled once — toSkeleton runs per unmatched/low-confidence row during clustering and
// reprocessing, so recompiling these on every call would waste cycles across large batches.
private val skeletonIdRegex = Regex("""\b[A-Z]{2,3}[A-Z0-9]{6,12}\b""")
private val skeletonAmountRegex = Regex("""[Kk][Ss][Hh]\s*[\d,]+(?:\.\d{1,2})?""")
private val skeletonPhoneRegex = Regex("""07\d{2}[*\d]{3}\d{3}""")
private val skeletonDateRegex = Regex("""\b\d{1,2}/\d{1,2}/\d{2,4}\b""")
private val skeletonTimeRegex = Regex("""\b\d{1,2}:\d{2}\s*[APap][Mm]\b""")
private val skeletonNumRegex = Regex("""\b\d{6,}\b""")
private val skeletonSpacesRegex = Regex("""\s{2,}""")

/**
 * Background service that runs the two learning-loop operations:
 *
 * 1. **Re-processing** ([reprocessOutdated])
 *    After bumping [PARSER_VERSION], call this to attempt reparsing all rows
 *    stored by older parser versions.  Successfully reparsed rows are promoted
 *    to real transactions and removed from UnmatchedSms.
 *
 * 2. **Structural clustering** ([clusterByStructure])
 *    Groups messages by their token-level "skeleton" to help you identify
 *    recurring patterns that deserve a new regex.  Results are written to
 *    [UnmatchedSms.patternGroup] so you can query them in the DAO.
 *
 * Both operations are safe to call repeatedly — they are idempotent.
 */
object ParserLearningService {

    // ── 1. Re-processing outdated rows ───────────────────────────────────────

    /**
     * Fetch every [UnmatchedSms] row whose [UnmatchedSms.parserVersion] is older
     * than [PARSER_VERSION] and attempt to reparse it.
     *
     * Outcomes per row:
     *   - [RichParseResult.FullMatch]      → insert real transaction, delete UnmatchedSms row
     *   - [RichParseResult.LowConfidence]  → update confidence + extractedFields, bump parserVersion
     *   - [RichParseResult.Unmatched]      → just bump parserVersion (no regression)
     *   - [RichParseResult.Irrelevant]     → delete the row (no longer looks like M-PESA)
     *
     * @return [ReprocessSummary] with counts for each outcome.
     */
    suspend fun reprocessOutdated(db: AppDatabase): ReprocessSummary =
        withContext(Dispatchers.IO) {
            val dao = db.unmatchedSmsDao()
            val txDao = db.transactionDao()

            val outdated = dao.getOutdatedRows(PARSER_VERSION)
            Log.i(TAG, "Re-processing ${outdated.size} outdated rows (parser v$PARSER_VERSION)")

            var promoted = 0
            var improved = 0
            var unchanged = 0
            var discarded = 0

            for (row in outdated) {
                when (val result = parseMpesaMessageRich(row.rawBody, row.smsId, row.receivedAt)) {

                    is RichParseResult.FullMatch -> {
                        // 🎉 New regex picked it up — promote to real transaction
                        val inserted = db.withTransaction {
                            val inserted = txDao.insertTransactions(listOf(result.transaction)).single() != -1L
                            if (inserted) {
                                UpcomingPaymentService(db).suggest(listOf(result.transaction))
                                dao.delete(row)
                            } else {
                                dao.update(row.copy(parserVersion = PARSER_VERSION))
                            }
                            inserted
                        }
                        if (inserted) {
                            promoted++
                            Log.d(TAG, "Promoted: ${row.id} → tx ${result.transaction.id}")
                        } else {
                            unchanged++
                            Log.w(TAG, "Promotion conflict; SMS ${row.id} remains in review")
                        }
                    }

                    is RichParseResult.LowConfidence -> {
                        // Partial improvement — update metadata, bump version
                        dao.update(
                            row.copy(
                                confidence = result.confidence,
                                extractedFields = result.partial.toJson(),
                                parserVersion = PARSER_VERSION,
                                contentHash = contentHash(result.normalizedBody)
                            )
                        )
                        improved++
                    }

                    is RichParseResult.Unmatched -> {
                        // Still unmatched, but mark as processed by current version
                        dao.update(row.copy(parserVersion = PARSER_VERSION))
                        unchanged++
                    }

                    is RichParseResult.Irrelevant -> {
                        // No longer looks like M-PESA — clean up
                        dao.delete(row)
                        discarded++
                    }
                }
            }

            ReprocessSummary(promoted, improved, unchanged, discarded).also {
                Log.i(TAG, "Re-process complete: $it")
            }
        }

    data class ReprocessSummary(
        val promoted: Int,      // Moved to real transactions
        val improved: Int,      // Higher confidence now
        val unchanged: Int,     // Still unmatched
        val discarded: Int      // No longer M-PESA
    ) {
        val total get() = promoted + improved + unchanged + discarded
    }

    // ── 2. Structural clustering ─────────────────────────────────────────────

    /**
     * Groups ungrouped [UnmatchedSms] rows by their structural "skeleton" —
     * a version of the message where variable parts (amounts, IDs, dates, names)
     * are replaced with typed placeholders.
     *
     * Messages with the same skeleton are given the same [UnmatchedSms.patternGroup].
     * The group name is derived from the skeleton's hash so it's stable across runs.
     *
     * After this runs, query `getUngrouped()` → should be empty.
     * Query rows by patternGroup to see which structures recur most — those are
     * your best candidates for a new regex.
     *
     * @return map of patternGroup → count of members
     */
    suspend fun clusterByStructure(dao: UnmatchedSmsDao): Map<String, Int> =
        withContext(Dispatchers.IO) {
            val ungrouped = dao.getUngrouped()
            Log.i(TAG, "Clustering ${ungrouped.size} ungrouped messages")

            val groups = mutableMapOf<String, MutableList<UnmatchedSms>>()

            for (row in ungrouped) {
                val skeleton = toSkeleton(normalizeSmsBody(row.rawBody))
                val groupKey = "grp_${contentHash(skeleton).take(8)}"
                groups.getOrPut(groupKey) { mutableListOf() }.add(row)
            }

            // Write group assignments back
            for ((groupKey, members) in groups) {
                for (member in members) {
                    dao.assignPatternGroup(member.id, groupKey)
                }
                Log.d(TAG, "Group $groupKey: ${members.size} messages")
                if (members.size >= 3) {
                    Log.i(
                        TAG,
                        "🔍 Recurring pattern ($groupKey × ${members.size}):\n${
                            toSkeleton(normalizeSmsBody(members.first().rawBody))
                        }"
                    )
                }
            }

            groups.mapValues { it.value.size }
        }

    /**
     * Convert a normalized message body into a structural skeleton by replacing
     * variable tokens with typed placeholders.
     *
     * Examples:
     *   "OKI12345678 Confirmed. Ksh450.00 sent to John Doe 0712345678 on 12/3/24 at 2:30 PM"
     *   → "<ID> Confirmed. Ksh<AMT> sent to <NAME> <PHONE> on <DATE> at <TIME>"
     *
     * This lets structurally identical messages (same grammar, different values)
     * land in the same cluster.
     */
    private fun toSkeleton(normalized: String): String = normalized
        // Transaction IDs (e.g. OKI12345678, SIN87654321)
        .replace(skeletonIdRegex, "<ID>")
        // Amounts (e.g. Ksh1,200.00, KSH450)
        .replace(skeletonAmountRegex, "Ksh<AMT>")
        // Phone numbers (e.g. 0712345678, 07123**678)
        .replace(skeletonPhoneRegex, "<PHONE>")
        // Dates (e.g. 12/3/24, 1/12/2024)
        .replace(skeletonDateRegex, "<DATE>")
        // Times (e.g. 2:30 PM, 11:05 AM)
        .replace(skeletonTimeRegex, "<TIME>")
        // Long digit sequences (account numbers, etc.)
        .replace(skeletonNumRegex, "<NUM>")
        // Normalise multiple spaces introduced by replacements
        .replace(skeletonSpacesRegex, " ")
        .trim()
}