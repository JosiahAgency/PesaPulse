package com.example.cashia.utility

import com.example.cashia.models.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FinancialReliabilityTest {
    private val sms = UnmatchedSms(id = 1, smsId = 42, rawBody = "Unrecognised M-PESA SMS")
    private val correction = TransactionCorrection(
        amount = "1,000.50", recipient = "Jane Wanjiku", date = "2024-02-29", type = "expense"
    )
    private val payment = UpcomingTransaction(
        id = "bill", title = "Jane Wanjiku", amount = 1000.50, categoryId = "OTHER",
        dueDate = "2024-02-29", type = UpcomingType.EXPENSE, recurrence = RecurrenceType.MONTHLY
    )

    @Test fun blankOptionalFieldsRemainUnknownIncludingCsv() {
        val tx = correction.toTransaction(sms)
        assertNull(tx.time)
        assertNull(tx.balance)
        assertNull(tx.transactionCost)
        assertNull(tx.reference)
        assertEquals("Unknown time", tx.displayTime)
        assertEquals("sms-correction:42", tx.id)
        for (column in listOf(ExportColumn.TIME, ExportColumn.BALANCE, ExportColumn.TRANSACTION_COST, ExportColumn.TRANSACTION_ID)) {
            assertEquals("", column.extract(tx, emptyMap()))
        }
        assertEquals(-1000.50, tx.netAmount, 0.001)
    }

    @Test fun actualZeroFeesAndBalanceRemainKnown() {
        val tx = correction.copy(fees = "0", balance = "0.00", time = "00:00", reference = "ABC1234567").toTransaction(sms)
        assertEquals(0.0, tx.transactionCost!!, 0.0)
        assertEquals(0.0, tx.balance!!, 0.0)
        assertEquals("12:00 AM", tx.time)
        assertEquals("ABC1234567", tx.reference)
        assertEquals("0.00", ExportColumn.TRANSACTION_COST.extract(tx, emptyMap()))
    }

    @Test fun invalidCorrectionsCannotBecomeFinancialRecords() {
        val invalid = listOf(
            correction.copy(amount = ""), correction.copy(amount = "NaN"), correction.copy(amount = "Infinity"),
            correction.copy(amount = "-1"), correction.copy(amount = "0"), correction.copy(amount = "1,2"),
            correction.copy(amount = "1.234"), correction.copy(recipient = " "),
            correction.copy(date = "2023-02-29"), correction.copy(date = ""),
            correction.copy(time = "24:00"), correction.copy(time = "12:72"),
            correction.copy(fees = "-0.01"), correction.copy(balance = "NaN"),
            correction.copy(reference = "invalid"), correction.copy(type = "")
        )
        for (draft in invalid) assertThrows(draft.toString(), IllegalArgumentException::class.java) {
            draft.toTransaction(sms)
        }
    }

    @Test fun matchingRequiresDirectionRecipientAmountAndDate() {
        val tx = correction.toTransaction(sms)
        assertNotNull(paymentMatchReason(payment, tx))
        assertNull(paymentMatchReason(payment, tx.copy(type = "income")))
        assertNull(paymentMatchReason(payment, tx.copy(recipient = "Jane Kamau")))
        assertNull(paymentMatchReason(payment.copy(notes = "Jane Wanjiku"), tx.copy(recipient = "Other Merchant")))
        assertNull(paymentMatchReason(payment, tx.copy(amount = 1100.0)))
        assertNull(paymentMatchReason(payment, tx.copy(date = "2024-03-08")))
        assertNull(paymentMatchReason(payment, tx.copy(reversedTxId = "original")))
        assertNull(paymentMatchReason(payment.copy(type = UpcomingType.SAVING), tx))
        assertNull(paymentMatchReason(payment.copy(status = UpcomingStatus.PAID), tx))
    }

    @Test fun matchingBoundariesAreInclusiveButDoNotUseSubstrings() {
        val exact = payment.copy(amount = 1000.0)
        val tx = correction.toTransaction(sms).copy(amount = 1050.0, date = "2024-03-07")
        assertNotNull(paymentMatchReason(exact, tx))
        assertNull(paymentMatchReason(exact, tx.copy(amount = 1050.01)))
        assertNull(paymentMatchReason(exact.copy(title = "Jane"), tx))
        assertNull(paymentMatchReason(exact.copy(title = "Wanjik"), tx))
    }

    @Test fun monthlyAndYearlyRecurrenceNeverDriftAndRetainOverdueDates() {
        val jan = LocalDate.parse("2024-01-31")
        val feb = nextRecurringDate(jan, RecurrenceType.MONTHLY, jan)
        assertEquals(LocalDate.parse("2024-02-29"), feb)
        assertEquals(LocalDate.parse("2024-03-31"), nextRecurringDate(feb, RecurrenceType.MONTHLY, jan))
        val leap = LocalDate.parse("2024-02-29")
        var due = leap
        repeat(4) { due = nextRecurringDate(due, RecurrenceType.YEARLY, leap) }
        assertEquals(LocalDate.parse("2028-02-29"), due)
        assertEquals(LocalDate.parse("2024-03-29"), payment.nextOccurrence())
    }

    @Test fun notificationRoutesRejectMalformedOrUnregisteredDestinations() {
        assertEquals("summary_report/month/2024-02-01", validatedNotificationRoute("summary_report/month/2024-02-01"))
        assertEquals("notifications_settings", validatedNotificationRoute("notifications_settings"))
        for (route in listOf("summary_report/year/2024-01-01", "summary_report/month/2023-02-29",
            "summary_report/day/2024-01-01/extra", "https://example.com", "unknown", "summary_report/day/today")) {
            assertNull(validatedNotificationRoute(route))
        }
    }

    @Test fun parserRejectsInvalidDatesInsteadOfInventingCurrentTimestamp() {
        val invalid = "ABC1234567 Confirmed. Ksh1,000.00 sent to Jane Wanjiku on 31/2/24 at 1:30 PM."
        assertTrue(parseMpesaMessage(invalid, 1) is ParseResult.Unmatched)
        val fourDigitYear = invalid.replace("31/2/24", "1/1/2024")
        val tx = (parseMpesaMessage(fourDigitYear, 1) as ParseResult.Success).transactions
        assertEquals("2024-01-01", tx.date)
        assertNull(tx.balance)
        assertNull(tx.transactionCost)
    }

    @Test fun dateOnlyBalanceSnapshotDoesNotInventATime() {
        val result = parseMpesaMessage("ABC1234567 Confirmed. Your M-PESA balance was Ksh500.00 on 1/1/24", 1)
        val tx = (result as ParseResult.Success).transactions
        assertNull(tx.time)
        assertEquals("2024-01-01", tx.date)
        assertEquals(500.0, tx.balance!!, 0.0)
    }
}
