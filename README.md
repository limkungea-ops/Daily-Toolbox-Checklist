# Daily Toolbox Checklist (Android)

Tool Box 사용 전/후 수량점검 Google Form 입력을 자동 채움으로 줄이는 안드로이드 앱.
기획안: [docs/PLAN.md](docs/PLAN.md)

## 현재 상태 (1단계)
- 설정 화면: Tool Box No, Hold Count, Shift, 사용자, 이메일 포함, 메일전송 방식, 폼/시트 주소 저장
- 점검 화면: Before/After 와 Abnormal 만 입력 → 값이 채워진 폼을 Chrome 으로 열기
- 제출 후 앱 복귀 시 "제출 완료" 확인 → 중복 제출 경고용 기록 + Tool Box 로그 시트 열기

회사 폼/시트 주소는 저장소에 넣지 않고 앱 설정 화면에서 입력합니다.

## 빌드
```bash
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew testDebugUnitTest assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions(`.github/workflows/build-apk.yml`)가 push 마다 테스트와 APK 를 빌드합니다.
GitHub 저장소의 Actions 탭 → 실행 → Artifacts 의 `toolbox-debug-apk` 에서 내려받습니다.

## 폼 항목 ID
폼이 수정되면 `app/src/main/java/com/orion/toolbox/logic/FormLogic.kt` 의 `FormSpec` 을 갱신합니다.
