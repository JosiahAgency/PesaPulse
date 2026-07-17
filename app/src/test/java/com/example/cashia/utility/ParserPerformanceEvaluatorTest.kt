package com.example.cashia.utility

import com.example.cashia.models.parseMpesaMessage
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class ParserPerformanceEvaluatorTest {

    @Test
    fun parserAverageLatencyStaysWithinGuardrail() {
        val sampleMessages = listOf(
            "QWE123ABC4 Confirmed. Ksh1,250.00 sent to John Doe 0712345678 on 17/7/26 at 10:20 AM. New M-PESA balance is Ksh10,500.00. Transaction cost, Ksh12.00.",
            "XYZ987RTY1 Confirmed. You have received Ksh2,000.00 from Jane Doe 0700111222 on 17/7/26 at 11:05 AM. New M-PESA balance is Ksh12,488.00.",
            "AAA111BBB2 Confirmed. Ksh450.00 paid to SHOP123 on 17/7/26 at 12:35 PM. New M-PESA balance is Ksh12,038.00. Transaction cost, Ksh0.00."
        )

        val warmupIterations = 200
        val measuredIterations = 2000

        repeat(warmupIterations) { index ->
            val message = sampleMessages[index % sampleMessages.size]
            parseMpesaMessage(message = message, smsId = index.toLong())
        }

        val elapsedNs = measureNanoTime {
            repeat(measuredIterations) { index ->
                val message = sampleMessages[index % sampleMessages.size]
                parseMpesaMessage(message = message, smsId = (index + warmupIterations).toLong())
            }
        }

        val avgMicrosPerParse = elapsedNs / measuredIterations / 1_000.0
        val guardrailMicros = 4_000.0

        assertTrue(
            "Parser average latency regression: ${"%.2f".format(avgMicrosPerParse)}us > ${"%.0f".format(guardrailMicros)}us",
            avgMicrosPerParse <= guardrailMicros
        )
    }
}
