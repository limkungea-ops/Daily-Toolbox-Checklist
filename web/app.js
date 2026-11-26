(function () {
  "use strict";
  var L = window.FormLogic;
  var SETTINGS_KEY = "toolbox.settings.v1";
  var HISTORY_KEY = "toolbox.history.v1";
  var MAIL_MODES = [
    ["OFF", "보내지 않음"],
    ["ALWAYS", "항상 체크"],
    ["ON_ABNORMAL", "Abnormal 이 0보다 클 때만 체크"],
  ];
  var DEFAULTS = { toolBoxNo: "", holdCount: "", shift: "Day", userName: "", recordEmail: true,
    mailMode: "ON_ABNORMAL", formUrl: "", logSheetUrl: "", manualSheetUrl: "" };

  // ---- 저장소 (사생활 보호 모드 등에서 실패해도 앱은 동작) ----
  var memory = {};
  function read(key) {
    try { var v = localStorage.getItem(key); if (v !== null) return v; } catch (e) {}
    return memory[key] === undefined ? null : memory[key];
  }
  function write(key, value) {
    memory[key] = value;
    try { localStorage.setItem(key, value); } catch (e) {}
  }
  function loadSettings() {
    try { return Object.assign({}, DEFAULTS, JSON.parse(read(SETTINGS_KEY) || "{}")); }
    catch (e) { return Object.assign({}, DEFAULTS); }
  }
  function loadHistory() {
    try { var h = JSON.parse(read(HISTORY_KEY) || "[]"); return Array.isArray(h) ? h : []; }
    catch (e) { return []; }
  }
  function addHistory(key) {
    var cutoff = isoDate(new Date(Date.now() - 90 * 86400000));
    var h = loadHistory().filter(function (k) { return k.split("|")[0] >= cutoff; });
    if (h.indexOf(key) < 0) h.push(key);
    write(HISTORY_KEY, JSON.stringify(h));
  }

  function isoDate(d) {
    var m = String(d.getMonth() + 1).padStart(2, "0"), day = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + m + "-" + day;
  }
  function $(id) { return document.getElementById(id); }

  var settings = loadSettings();
  var state = { phase: null, lossType: null, pending: null, leftPage: false };

  // ---- 탭 ----
  function showTab(name) {
    $("tab-check").hidden = name !== "check";
    $("tab-settings").hidden = name !== "settings";
    $("nav-check").classList.toggle("active", name === "check");
    $("nav-settings").classList.toggle("active", name === "settings");
    if (name === "check") renderCheck(); else fillSettingsForm();
    window.scrollTo(0, 0);
  }
  $("nav-check").onclick = function () { showTab("check"); };
  $("nav-settings").onclick = function () { showTab("settings"); };
  document.querySelector("[data-go=settings]").onclick = function () { showTab("settings"); };

  // ---- 칩/라디오 헬퍼 ----
  function renderChips(container, options, selected, onPick) {
    container.innerHTML = "";
    options.forEach(function (o) {
      var b = document.createElement("button");
      b.type = "button"; b.className = "chip"; b.textContent = o;
      b.setAttribute("role", "radio"); b.setAttribute("aria-checked", String(o === selected));
      b.onclick = function () { onPick(o); };
      container.appendChild(b);
    });
  }
  function renderRadios(container, name, options, selected, onPick) {
    container.innerHTML = "";
    options.forEach(function (o) {
      var value = Array.isArray(o) ? o[0] : o, label = Array.isArray(o) ? o[1] : o;
      var l = document.createElement("label"); l.className = "opt";
      var r = document.createElement("input"); r.type = "radio"; r.name = name; r.value = value;
      r.checked = value === selected; r.onchange = function () { onPick(value); };
      l.appendChild(r); l.appendChild(document.createTextNode(label));
      container.appendChild(l);
    });
  }

  // ---- 점검 화면 ----
  function setupReady() {
    var e = L.validateSettings(settings);
    return !(e.toolBoxNo || e.holdCount || e.formUrl);
  }
  function currentCheckin() {
    var n = parseInt($("abnormal").value, 10);
    return { phase: effectivePhase(), abnormal: isNaN(n) || n < 0 ? 0 : n,
      lossType: state.lossType, remark: $("remark").value };
  }
  function effectivePhase() {
    if (state.phase) return state.phase;
    var done = loadHistory().indexOf(L.historyKey(isoDate(new Date()), settings.shift, "Before")) >= 0;
    return done ? "After" : "Before";
  }
  function renderCheck() {
    var ready = setupReady();
    $("need-setup").hidden = ready;
    $("check-body").hidden = !ready;
    $("open-log").disabled = !settings.logSheetUrl.trim();
    $("open-manual").disabled = !settings.manualSheetUrl.trim();
    if (!ready) return;

    var s = $("summary");
    s.textContent = "";
    [["자동 입력 값 (설정)", null], ["Tool Box No: " + settings.toolBoxNo, 1], ["Hold Count: " + settings.holdCount, 1],
      ["Shift: " + settings.shift, 1]].concat(settings.userName ? [["사용자: " + settings.userName, 1]] : [])
      .concat([["날짜: " + isoDate(new Date()), 1]]).forEach(function (row) {
        var d = document.createElement(row[1] ? "div" : "strong"); d.textContent = row[0]; s.appendChild(d);
      });

    renderChips($("phase-chips"), ["Before", "After"], effectivePhase(), function (p) { state.phase = p; renderCheck(); });
    var c = currentCheckin();
    $("abnormal-extra").hidden = c.abnormal <= 0;
    renderRadios($("loss-types"), "loss", L.LOSS_TYPES, state.lossType, function (v) { state.lossType = v; });
    $("mail-hint").hidden = !L.shouldSendMail(settings, c);
  }
  $("abnormal").oninput = function () {
    var c = currentCheckin();
    $("abnormal-extra").hidden = c.abnormal <= 0;
    $("mail-hint").hidden = !L.shouldSendMail(settings, c);
  };

  function launchForm(c) {
    var url = L.buildPrefillUrl(settings, c);
    if (!url) return;
    state.pending = L.historyKey(isoDate(new Date()), settings.shift, c.phase);
    state.leftPage = false;
    window.open(url, "_blank", "noopener");
  }
  $("submit").onclick = function () {
    var c = currentCheckin();
    var errs = L.validateCheckin(c);
    $("err-lossType").textContent = errs.lossType || "";
    $("err-remark").textContent = errs.remark || "";
    if (errs.lossType || errs.remark) return;
    var dup = loadHistory().indexOf(L.historyKey(isoDate(new Date()), settings.shift, c.phase)) >= 0;
    if (dup) {
      dialog("이미 제출한 기록이 있습니다",
        "오늘 " + settings.shift + " " + c.phase + " 점검은 이미 제출했습니다. 다시 제출할까요?",
        "취소", "다시 제출", null, function () { launchForm(c); });
    } else {
      launchForm(c);
    }
  };
  function openSheet(url) { if (url.trim()) window.open(url.trim(), "_blank", "noopener"); }
  $("open-log").onclick = function () { openSheet(settings.logSheetUrl); };
  $("open-manual").onclick = function () { openSheet(settings.manualSheetUrl); };

  // 폼을 열었다가 돌아오면 제출 완료 여부를 묻는다
  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "hidden") { state.leftPage = true; return; }
    if (state.leftPage && state.pending) {
      state.leftPage = false;
      dialog("제출을 완료하셨나요?", "완료하면 기록에 저장하고 Tool Box 로그 시트를 보여드립니다.",
        "아직 안 함", "제출 완료",
        function () { state.pending = null; },
        function () { addHistory(state.pending); state.pending = null; openSheet(settings.logSheetUrl); renderCheck(); });
    }
  });

  // ---- 다이얼로그 ----
  function dialog(title, text, noLabel, yesLabel, onNo, onYes) {
    $("dlg-title").textContent = title; $("dlg-text").textContent = text;
    $("dlg-no").textContent = noLabel; $("dlg-yes").textContent = yesLabel;
    $("dialog").hidden = false;
    function close() { $("dialog").hidden = true; }
    $("dlg-no").onclick = function () { close(); if (onNo) onNo(); };
    $("dlg-yes").onclick = function () { close(); if (onYes) onYes(); };
  }

  // ---- 설정 화면 ----
  var draft = Object.assign({}, settings);
  function fillSettingsForm() {
    draft = Object.assign({}, settings);
    ["toolBoxNo", "holdCount", "userName", "formUrl", "logSheetUrl", "manualSheetUrl"].forEach(function (k) {
      $("s-" + k).value = draft[k]; $("e-" + k).textContent = "";
    });
    $("s-recordEmail").checked = !!draft.recordEmail;
    renderChips($("shift-chips"), ["Day", "Night"], draft.shift, function (v) { draft.shift = v; syncDraftFields(); drawShift(); });
    renderRadios($("mail-modes"), "mail", MAIL_MODES, draft.mailMode, function (v) { draft.mailMode = v; });
    $("save-msg").textContent = "";
  }
  function drawShift() {
    renderChips($("shift-chips"), ["Day", "Night"], draft.shift, function (v) { draft.shift = v; syncDraftFields(); drawShift(); });
  }
  function syncDraftFields() {
    ["toolBoxNo", "holdCount", "userName", "formUrl", "logSheetUrl", "manualSheetUrl"].forEach(function (k) {
      draft[k] = $("s-" + k).value;
    });
    draft.recordEmail = $("s-recordEmail").checked;
  }
  $("save").onclick = function () {
    syncDraftFields();
    var errs = L.validateSettings(draft);
    ["toolBoxNo", "holdCount", "userName", "formUrl", "logSheetUrl", "manualSheetUrl"].forEach(function (k) {
      $("e-" + k).textContent = errs[k] || "";
    });
    if (Object.keys(errs).length) { $("save-msg").textContent = "입력 내용을 확인하세요."; return; }
    draft.toolBoxNo = draft.toolBoxNo.trim(); draft.holdCount = draft.holdCount.trim();
    draft.userName = draft.userName.trim(); draft.logSheetUrl = draft.logSheetUrl.trim();
    draft.manualSheetUrl = draft.manualSheetUrl.trim();
    draft.formUrl = L.normalizeFormUrl(draft.formUrl) || draft.formUrl.trim();
    settings = Object.assign({}, draft);
    write(SETTINGS_KEY, JSON.stringify(settings));
    $("s-formUrl").value = settings.formUrl;
    $("save-msg").textContent = "저장했습니다.";
  };

  // ---- 서비스 워커 (오프라인 실행) ----
  if ("serviceWorker" in navigator && (location.protocol === "https:" || location.hostname === "localhost")) {
    navigator.serviceWorker.register("sw.js").catch(function () {});
  }

  showTab(setupReady() ? "check" : "settings");
})();
