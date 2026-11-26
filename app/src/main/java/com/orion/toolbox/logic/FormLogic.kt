package com.orion.toolbox.logic

import java.net.URLEncoder
import java.time.LocalDate

/**
 * Google Form 의 항목 ID (저장된 폼 스냅샷에서 추출).
 * 폼이 수정되면 ID 가 바뀔 수 있으므로 한 곳에 모아 둔다.
 */
object FormSpec {
    const val ENTRY_TOOLBOX_NO = "entry.2099377435"
    const val ENTRY_PHASE = "entry.1506871634"
    const val ENTRY_SHIFT = "entry.419958790"
    const val ENTRY_HOLD_COUNT = "entry.1109690147"
    const val ENTRY_ABNORMAL = "entry.1266075164"
    const val ENTRY_USER = "entry.180368103"
    const val ENTRY_LOSS_TYPE = "entry.2074485093"
    const val ENTRY_REMARK = "entry.147453066"
    const val ENTRY_SEND_MAIL = "entry.703810429"
    const val SEND_MAIL_VALUE = "메일전송"

    val LOSS_TYPES = listOf("분실공구", "파손공구", "대여공구", "Finding", "기타")
}

enum class SettingsField { TOOLBOX_NO, HOLD_COUNT, USER_NAME, FORM_URL, LOG_SHEET_URL, MANUAL_SHEET_URL }

data class CheckinErrors(val lossType: String? = null, val remark: String? = null) {
    val hasError get() = lossType != null || remark != null
}

object FormLogic {
    private val FORM_URL_REGEX =
        Regex("""^https://docs\.google\.com/forms/(u/\d+/)?d/e/[A-Za-z0-9_-]+/(viewform|formResponse)([?#].*)?$""")
    private val SHEET_URL_REGEX = Regex("""^https://docs\.google\.com/spreadsheets/.+""")
    private val USER_NAME_REGEX = Regex("""^[가-힣]{1,4}$""")

    fun isValidFormUrl(url: String) = FORM_URL_REGEX.matches(url.trim())

    fun isValidSheetUrl(url: String) = SHEET_URL_REGEX.matches(url.trim())

    /** 사용자가 붙여넣은 폼 주소를 `.../viewform` 기본 주소로 정리한다. 형식이 틀리면 null. */
    fun normalizeFormUrl(url: String): String? {
        val trimmed = url.trim()
        if (!isValidFormUrl(trimmed)) return null
        return trimmed
            .substringBefore('#')
            .substringBefore('?')
            .replace(Regex("/forms/u/\\d+/"), "/forms/")
            .removeSuffix("/formResponse")
            .removeSuffix("/viewform") + "/viewform"
    }

    fun validateSettings(s: AppSettings): Map<SettingsField, String> {
        val errors = linkedMapOf<SettingsField, String>()
        if (s.toolBoxNo.isBlank()) {
            errors[SettingsField.TOOLBOX_NO] = "Tool Box No 를 입력하세요 (폼 목록과 동일하게)"
        }
        val hold = s.holdCount.trim().toIntOrNull()
        if (hold == null || hold < 0) {
            errors[SettingsField.HOLD_COUNT] = "Hold Count 는 0 이상의 정수여야 합니다"
        }
        if (s.userName.isNotBlank() && !USER_NAME_REGEX.matches(s.userName.trim())) {
            errors[SettingsField.USER_NAME] = "한글 이름 최대 4자리만 입력 가능합니다"
        }
        if (!isValidFormUrl(s.formUrl)) {
            errors[SettingsField.FORM_URL] = "Google Form 주소(.../viewform)를 붙여넣으세요"
        }
        if (s.logSheetUrl.isNotBlank() && !isValidSheetUrl(s.logSheetUrl)) {
            errors[SettingsField.LOG_SHEET_URL] = "Google 시트 주소가 아닙니다"
        }
        if (s.manualSheetUrl.isNotBlank() && !isValidSheetUrl(s.manualSheetUrl)) {
            errors[SettingsField.MANUAL_SHEET_URL] = "Google 시트 주소가 아닙니다"
        }
        return errors
    }

    /** 폼 규칙과 동일: Abnormal > 0 이면 유형과 Remark 필수 */
    fun validateCheckin(c: Checkin): CheckinErrors {
        if (c.abnormal <= 0) return CheckinErrors()
        return CheckinErrors(
            lossType = if (c.lossType.isNullOrBlank()) "유형을 선택하세요" else null,
            remark = if (c.remark.isBlank()) "분실 공구가 있으면 Remark 를 입력해야 합니다" else null,
        )
    }

    fun shouldSendMail(s: AppSettings, c: Checkin) = when (s.mailMode) {
        MailMode.OFF -> false
        MailMode.ALWAYS -> true
        MailMode.ON_ABNORMAL -> c.abnormal > 0
    }

    /** 값이 미리 채워진 폼 주소. 폼 주소가 올바르지 않으면 null. */
    fun buildPrefillUrl(s: AppSettings, c: Checkin): String? {
        val base = normalizeFormUrl(s.formUrl) ?: return null
        val params = mutableListOf("usp" to "pp_url")
        params += FormSpec.ENTRY_TOOLBOX_NO to s.toolBoxNo.trim()
        params += FormSpec.ENTRY_PHASE to c.phase.label
        params += FormSpec.ENTRY_SHIFT to s.shift.label
        params += FormSpec.ENTRY_HOLD_COUNT to s.holdCount.trim()
        params += FormSpec.ENTRY_ABNORMAL to c.abnormal.toString()
        if (s.userName.isNotBlank()) params += FormSpec.ENTRY_USER to s.userName.trim()
        if (c.abnormal > 0 && !c.lossType.isNullOrBlank()) params += FormSpec.ENTRY_LOSS_TYPE to c.lossType
        if (c.remark.isNotBlank()) params += FormSpec.ENTRY_REMARK to c.remark.trim()
        if (shouldSendMail(s, c)) params += FormSpec.ENTRY_SEND_MAIL to FormSpec.SEND_MAIL_VALUE
        // 폼의 "이메일을 응답에 포함" 체크박스 (실기기에서 동작 확인 필요)
        if (s.recordEmail) params += "emailReceipt" to "true"
        return base + "?" + params.joinToString("&") { (k, v) -> "$k=${encode(v)}" }
    }

    private fun encode(v: String) = URLEncoder.encode(v, "UTF-8").replace("+", "%20")

    /** 중복 제출 확인용 키: 날짜|Shift|Before/After */
    fun historyKey(date: LocalDate, shift: Shift, phase: Phase) =
        "$date|${shift.name}|${phase.name}"
}
