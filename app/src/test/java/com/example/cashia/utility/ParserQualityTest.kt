package com.example.cashia.utility

import com.example.cashia.models.ParseResult
import com.example.cashia.models.parseMpesaMessage
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserQualityTest {

    @Test
    fun parsesSendMoneySmsAsTransaction() {
        val message =
            "QWE123ABC4 Confirmed. Ksh1,250.00 sent to John Doe 0712345678 on 17/7/26 at 10:20 AM. New M-PESA balance is Ksh10,500.00. Transaction cost, Ksh12.00."

        val result = parseMpesaMessage(message = message, smsId = 1L)

        assertTrue("Expected successful parse for valid send-money SMS", result is ParseResult.Success)
    }

    @Test
    fun ignoresNonMpesaMessage() {
        val message = "Your OTP for login is 483920. It expires in 2 minutes."

        val result = parseMpesaMessage(message = message, smsId = 2L)

        assertTrue("Expected irrelevant classification for non-M-PESA message", result is ParseResult.Irrelevant)
    }
}
