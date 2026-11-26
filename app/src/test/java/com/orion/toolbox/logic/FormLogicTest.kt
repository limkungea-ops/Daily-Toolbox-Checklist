package com.orion.toolbox.logic

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormLogicTest {
    private val formUrl = "https://docs.google.com/forms/d/e/1FAIpQLSexample_ID-123/viewform?usp=sf_link"

    private val settings = AppSettings(
        toolBoxNo = "GMP617",
        holdCount = "25",
        shift = Shift.NIGHT,
        userName = "홍길동",
        recordEmail = true,
        mailMode = MailMode.ON_ABNORMAL,
        formUrl = formUrl,
    )

    @Test
    fun normalizeFormUrl_stripsQueryAndUserSegment() {
        assertEquals(
            "https://docs.google.com/forms/d/e/1FAIpQLSexample_ID-123/viewform",
            FormLogic.normalizeFormUrl("https://docs.google.com/forms/u/0/d/e/1FAIpQLSexample_ID-123/formResponse?x=1"),
        )
        assertNull(FormLogic.normalizeFormUrl("https://example.com/forms/d/e/abc/viewform"))
        assertNull(FormLogic.normalizeFormUrl(""))
    }

    @Test
    fun prefillUrl_normalCheckin() {
        val url = FormLogic.buildPrefillUrl(settings, Checkin(Phase.BEFORE, 0))!!
        assertTrue(url.startsWith("https://docs.google.com/forms/d/e/1FAIpQLSexample_ID-123/viewform?usp=pp_url"))
        assertTrue(url.contains("entry.2099377435=GMP617"))
        assertTrue(url.contains("entry.1506871634=Before"))
        assertTrue(url.contains("entry.419958790=Night"))
        assertTrue(url.contains("entry.1109690147=25"))
        assertTrue(url.contains("entry.1266075164=0"))
        assertTrue(url.contains("entry.180368103=%ED%99%8D%EA%B8%B8%EB%8F%99"))
        assertTrue(url.contains("emailReceipt=true"))
        assertFalse(url.contains("entry.703810429"))
        assertFalse(url.contains("entry.2074485093"))
    }

    @Test
    fun prefillUrl_abnormalAddsTypeRemarkAndMail() {
        val c = Checkin(Phase.AFTER, 2, "분실공구", "10mm 소켓 1, 드라이버 1")
        val url = FormLogic.buildPrefillUrl(settings, c)!!
        assertTrue(url.contains("entry.1506871634=After"))
        assertTrue(url.contains("entry.1266075164=2"))
        assertTrue(url.contains("entry.2074485093=%EB%B6%84%EC%8B%A4%EA%B3%B5%EA%B5%AC"))
        assertTrue(url.contains("entry.147453066="))
        assertTrue(url.contains("entry.703810429=%EB%A9%94%EC%9D%BC%EC%A0%84%EC%86%A1"))
        assertFalse("공백은 + 가 아니라 %20 으로 인코딩", url.contains("+"))
    }

    @Test
    fun prefillUrl_emailOffAndInvalidForm() {
        val off = settings.copy(recordEmail = false, userName = "")
        val url = FormLogic.buildPrefillUrl(off, Checkin(Phase.BEFORE, 0))!!
        assertFalse(url.contains("emailReceipt"))
        assertFalse(url.contains("entry.180368103"))
        assertNull(FormLogic.buildPrefillUrl(settings.copy(formUrl = "nope"), Checkin(Phase.BEFORE, 0)))
    }

    @Test
    fun mailMode() {
        val normal = Checkin(Phase.BEFORE, 0)
        val abnormal = Checkin(Phase.BEFORE, 1)
        assertFalse(FormLogic.shouldSendMail(settings.copy(mailMode = MailMode.OFF), abnormal))
        assertTrue(FormLogic.shouldSendMail(settings.copy(mailMode = MailMode.ALWAYS), normal))
        assertFalse(FormLogic.shouldSendMail(settings.copy(mailMode = MailMode.ON_ABNORMAL), normal))
        assertTrue(FormLogic.shouldSendMail(settings.copy(mailMode = MailMode.ON_ABNORMAL), abnormal))
    }

    @Test
    fun validateCheckin_abnormalRequiresTypeAndRemark() {
        assertFalse(FormLogic.validateCheckin(Checkin(Phase.BEFORE, 0)).hasError)
        val e = FormLogic.validateCheckin(Checkin(Phase.AFTER, 1))
        assertNotNull(e.lossType)
        assertNotNull(e.remark)
        assertFalse(FormLogic.validateCheckin(Checkin(Phase.AFTER, 1, "파손공구", "스패너 파손")).hasError)
    }

    @Test
    fun validateSettings() {
        assertTrue(FormLogic.validateSettings(settings).isEmpty())
        val bad = AppSettings(userName = "홍길동이다", logSheetUrl = "http://x")
        val errors = FormLogic.validateSettings(bad)
        assertTrue(SettingsField.TOOLBOX_NO in errors)
        assertTrue(SettingsField.HOLD_COUNT in errors)
        assertTrue(SettingsField.USER_NAME in errors)
        assertTrue(SettingsField.FORM_URL in errors)
        assertTrue(SettingsField.LOG_SHEET_URL in errors)
        assertFalse(SettingsField.MANUAL_SHEET_URL in errors)
    }

    @Test
    fun historyKey() {
        assertEquals("2026-11-26|DAY|BEFORE", FormLogic.historyKey(LocalDate.of(2026, 11, 26), Shift.DAY, Phase.BEFORE))
    }
}
