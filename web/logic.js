/* Google Form 미리채움 로직 (안드로이드 앱 FormLogic.kt 와 동일 규칙) */
(function (root, factory) {
  if (typeof module === "object" && module.exports) module.exports = factory();
  else root.FormLogic = factory();
})(typeof self !== "undefined" ? self : this, function () {
  // 폼 항목 ID: 폼이 수정되면 여기만 갱신한다
  var ENTRY = {
    toolBoxNo: "entry.2099377435",
    phase: "entry.1506871634",
    shift: "entry.419958790",
    holdCount: "entry.1109690147",
    abnormal: "entry.1266075164",
    user: "entry.180368103",
    lossType: "entry.2074485093",
    remark: "entry.147453066",
    sendMail: "entry.703810429",
  };
  var SEND_MAIL_VALUE = "메일전송";
  var LOSS_TYPES = ["분실공구", "파손공구", "대여공구", "Finding", "기타"];

  var FORM_URL_RE = /^https:\/\/docs\.google\.com\/forms\/(u\/\d+\/)?d\/e\/[A-Za-z0-9_-]+\/(viewform|formResponse)([?#].*)?$/;
  var SHEET_URL_RE = /^https:\/\/docs\.google\.com\/spreadsheets\/.+/;
  var USER_RE = /^[가-힣]{1,4}$/;

  function isValidFormUrl(u) { return FORM_URL_RE.test((u || "").trim()); }
  function isValidSheetUrl(u) { return SHEET_URL_RE.test((u || "").trim()); }

  function normalizeFormUrl(u) {
    u = (u || "").trim();
    if (!isValidFormUrl(u)) return null;
    u = u.split("#")[0].split("?")[0].replace(/\/forms\/u\/\d+\//, "/forms/");
    u = u.replace(/\/formResponse$/, "").replace(/\/viewform$/, "");
    return u + "/viewform";
  }

  function validateSettings(s) {
    var e = {};
    if (!(s.toolBoxNo || "").trim()) e.toolBoxNo = "Tool Box No 를 입력하세요 (폼 목록과 동일하게)";
    var hold = (s.holdCount || "").trim();
    if (!/^\d+$/.test(hold)) e.holdCount = "Hold Count 는 0 이상의 정수여야 합니다";
    if ((s.userName || "").trim() && !USER_RE.test(s.userName.trim())) e.userName = "한글 이름 최대 4자리만 입력 가능합니다";
    if (!isValidFormUrl(s.formUrl)) e.formUrl = "Google Form 주소(.../viewform)를 붙여넣으세요";
    if ((s.logSheetUrl || "").trim() && !isValidSheetUrl(s.logSheetUrl)) e.logSheetUrl = "Google 시트 주소가 아닙니다";
    if ((s.manualSheetUrl || "").trim() && !isValidSheetUrl(s.manualSheetUrl)) e.manualSheetUrl = "Google 시트 주소가 아닙니다";
    return e;
  }

  // Abnormal > 0 이면 유형과 Remark 필수 (폼 규칙과 동일)
  function validateCheckin(c) {
    var e = {};
    if (c.abnormal > 0) {
      if (!c.lossType) e.lossType = "유형을 선택하세요";
      if (!(c.remark || "").trim()) e.remark = "분실 공구가 있으면 Remark 를 입력해야 합니다";
    }
    return e;
  }

  function shouldSendMail(s, c) {
    if (s.mailMode === "ALWAYS") return true;
    if (s.mailMode === "ON_ABNORMAL") return c.abnormal > 0;
    return false;
  }

  function buildPrefillUrl(s, c) {
    var base = normalizeFormUrl(s.formUrl);
    if (!base) return null;
    var p = [["usp", "pp_url"]];
    p.push([ENTRY.toolBoxNo, s.toolBoxNo.trim()]);
    p.push([ENTRY.phase, c.phase]);
    p.push([ENTRY.shift, s.shift]);
    p.push([ENTRY.holdCount, s.holdCount.trim()]);
    p.push([ENTRY.abnormal, String(c.abnormal)]);
    if ((s.userName || "").trim()) p.push([ENTRY.user, s.userName.trim()]);
    if (c.abnormal > 0 && c.lossType) p.push([ENTRY.lossType, c.lossType]);
    if ((c.remark || "").trim()) p.push([ENTRY.remark, c.remark.trim()]);
    if (shouldSendMail(s, c)) p.push([ENTRY.sendMail, SEND_MAIL_VALUE]);
    if (s.recordEmail) p.push(["emailReceipt", "true"]); // 실기기에서 동작 확인 필요
    return base + "?" + p.map(function (kv) { return kv[0] + "=" + encodeURIComponent(kv[1]); }).join("&");
  }

  function historyKey(dateStr, shift, phase) { return dateStr + "|" + shift + "|" + phase; }

  return {
    ENTRY: ENTRY, LOSS_TYPES: LOSS_TYPES,
    isValidFormUrl: isValidFormUrl, isValidSheetUrl: isValidSheetUrl, normalizeFormUrl: normalizeFormUrl,
    validateSettings: validateSettings, validateCheckin: validateCheckin,
    shouldSendMail: shouldSendMail, buildPrefillUrl: buildPrefillUrl, historyKey: historyKey,
  };
});
