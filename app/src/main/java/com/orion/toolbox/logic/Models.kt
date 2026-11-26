package com.orion.toolbox.logic

enum class Phase(val label: String) { BEFORE("Before"), AFTER("After") }

enum class Shift(val label: String) { DAY("Day"), NIGHT("Night") }

/** 폼의 "Tool 담당자에게 특이사항 메일 전송" 체크박스 처리 방식 */
enum class MailMode(val label: String) {
    OFF("보내지 않음"),
    ALWAYS("항상 체크"),
    ON_ABNORMAL("Abnormal 이 0보다 클 때만 체크"),
}

data class AppSettings(
    val toolBoxNo: String = "",
    val holdCount: String = "",
    val shift: Shift = Shift.DAY,
    val userName: String = "",
    val recordEmail: Boolean = true,
    val mailMode: MailMode = MailMode.ON_ABNORMAL,
    val formUrl: String = "",
    val logSheetUrl: String = "",
    val manualSheetUrl: String = "",
)

/** 매일 점검 화면에서 입력하는 값 */
data class Checkin(
    val phase: Phase,
    val abnormal: Int,
    val lossType: String? = null,
    val remark: String = "",
)
