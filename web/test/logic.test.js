const test = require("node:test");
const assert = require("node:assert");
global.TOOLBOX_OPTIONS = ["GMP617", "GMP516", "EMS-안동"];
const L = require("../logic.js");

const form = "https://docs.google.com/forms/d/e/1FAIpQLSexample_ID-123/viewform?usp=sf_link";
const base = { toolBoxNo: "GMP617", holdCount: "25", shift: "Night", userName: "홍길동",
  recordEmail: true, mailMode: "ON_ABNORMAL", formUrl: form, logSheetUrl: "", manualSheetUrl: "" };

test("normalizeFormUrl", () => {
  assert.strictEqual(L.normalizeFormUrl("https://docs.google.com/forms/u/0/d/e/1FAIpQLSexample_ID-123/formResponse?x=1"),
    "https://docs.google.com/forms/d/e/1FAIpQLSexample_ID-123/viewform");
  assert.strictEqual(L.normalizeFormUrl("https://example.com/forms/d/e/abc/viewform"), null);
  assert.strictEqual(L.normalizeFormUrl(""), null);
});

test("prefill: normal", () => {
  const u = L.buildPrefillUrl(base, { phase: "Before", abnormal: 0, lossType: null, remark: "" });
  assert.ok(u.startsWith("https://docs.google.com/forms/d/e/1FAIpQLSexample_ID-123/viewform?usp=pp_url"));
  for (const part of ["entry.2099377435=GMP617", "entry.1506871634=Before", "entry.419958790=Night",
    "entry.1109690147=25", "entry.1266075164=0", "entry.180368103=%ED%99%8D%EA%B8%B8%EB%8F%99", "emailReceipt=true"])
    assert.ok(u.includes(part), part);
  assert.ok(!u.includes("entry.703810429"));
  assert.ok(!u.includes("entry.2074485093"));
});

test("prefill: abnormal adds type, remark, mail", () => {
  const u = L.buildPrefillUrl(base, { phase: "After", abnormal: 2, lossType: "분실공구", remark: "10mm 소켓 1" });
  assert.ok(u.includes("entry.1506871634=After"));
  assert.ok(u.includes("entry.2074485093=%EB%B6%84%EC%8B%A4%EA%B3%B5%EA%B5%AC"));
  assert.ok(u.includes("entry.147453066=10mm%20%EC%86%8C%EC%BC%93%201"));
  assert.ok(u.includes("entry.703810429="));
  assert.ok(!u.includes("+"));
});

test("prefill: email off, invalid form", () => {
  const u = L.buildPrefillUrl({ ...base, recordEmail: false, userName: "" }, { phase: "Before", abnormal: 0 });
  assert.ok(!u.includes("emailReceipt") && !u.includes("entry.180368103"));
  assert.strictEqual(L.buildPrefillUrl({ ...base, formUrl: "nope" }, { phase: "Before", abnormal: 0 }), null);
});

test("mail modes", () => {
  const n = { abnormal: 0 }, a = { abnormal: 1 };
  assert.strictEqual(L.shouldSendMail({ mailMode: "OFF" }, a), false);
  assert.strictEqual(L.shouldSendMail({ mailMode: "ALWAYS" }, n), true);
  assert.strictEqual(L.shouldSendMail({ mailMode: "ON_ABNORMAL" }, n), false);
  assert.strictEqual(L.shouldSendMail({ mailMode: "ON_ABNORMAL" }, a), true);
});

test("validate", () => {
  assert.deepStrictEqual(L.validateCheckin({ abnormal: 0 }), {});
  assert.deepStrictEqual(Object.keys(L.validateCheckin({ abnormal: 1, lossType: null, remark: "" })).sort(), ["lossType", "remark"]);
  assert.deepStrictEqual(L.validateSettings(base), {});
  const e = L.validateSettings({ userName: "홍길동이다", logSheetUrl: "http://x" });
  for (const k of ["toolBoxNo", "holdCount", "userName", "formUrl", "logSheetUrl"]) assert.ok(k in e, k);
  assert.ok(!("manualSheetUrl" in e));
});

test("historyKey", () => {
  assert.strictEqual(L.historyKey("2026-11-26", "Day", "Before"), "2026-11-26|Day|Before");
});

test("toolbox no must be in list; suggest GMP prefix", () => {
  assert.ok("toolBoxNo" in L.validateSettings({ ...base, toolBoxNo: "516" }));
  assert.ok(!("toolBoxNo" in L.validateSettings({ ...base, toolBoxNo: "GMP516" })));
  assert.strictEqual(L.suggestToolBoxNo("516"), "GMP516");
  assert.strictEqual(L.suggestToolBoxNo("gmp617"), "GMP617");
  assert.strictEqual(L.suggestToolBoxNo("999"), "999");
});

test("email prefill params", () => {
  const u = L.buildPrefillUrl({ ...base, email: "a@b.com" }, { phase: "Before", abnormal: 0 });
  assert.ok(u.includes("emailAddress=a%40b.com") && u.includes("emailReceipt=true"));
  assert.ok("email" in L.validateSettings({ ...base, email: "bad" }));
});
