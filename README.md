# Reverse Portrait Guard

Android의 중력 센서로 Reverse Portrait를 감지해 현재 Activity의 터치를 차단하는 예제입니다.

- 자동 회전 설정과 무관하게 동작
- 300ms 디바운스와 진입/해제 히스테리시스 적용
- `TYPE_GRAVITY` 미지원 시 가속도 센서 + 저역 통과 필터 사용
- 별도 센서 권한 불필요
- Foreground Service로 다른 앱 사용 중에도 방향 감지
- Reverse Portrait에서 전체 화면 오버레이로 일반 앱 터치 차단
- 잠금화면 위 Activity 표시 지원 (`setShowWhenLocked`)

앱 실행 후 **다른 앱 위에 표시** 권한을 허용하고 **터치 방지 시작**을 누르세요.
시스템 내비게이션, 전원/볼륨 버튼, 잠금화면 보안 UI는 차단하지 않습니다.

빌드 결과: `app/build/outputs/apk/debug/app-debug.apk`

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

Release 빌드:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Configuration Release
```
