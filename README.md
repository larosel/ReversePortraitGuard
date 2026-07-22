# Reverse Portrait Guard

Android 기기가 Reverse Portrait(상하 반전 세로 방향) 상태일 때 실수로 발생하는 터치를 차단하는 개인용 앱입니다. 자동 회전 설정과 무관하게 중력 센서로 기기 방향을 감지합니다.

## 주요 기능

- `TYPE_GRAVITY` 센서로 Reverse Portrait 감지
- 중력 센서 미지원 시 가속도 센서와 저역 통과 필터 사용
- 300ms 디바운스와 진입/해제 히스테리시스로 오작동 감소
- 차단 메시지를 180° 회전해 거꾸로 든 상태에서 똑바로 표시
- 일반 앱 사용 중 전체 화면 터치 차단
- 잠금화면에서 전체 화면 터치 차단

## 동작 구조

### 일반 앱 화면

Foreground Service가 백그라운드에서 방향을 감지합니다. Reverse Portrait가 감지되면 `TYPE_APPLICATION_OVERLAY` 전체 화면을 표시해 일반 앱 영역의 터치를 차단합니다.

### 잠금화면

접근성 서비스가 잠금 상태와 기기 방향을 확인합니다. 잠금 상태에서 Reverse Portrait가 감지되면 `TYPE_ACCESSIBILITY_OVERLAY` 전체 화면을 표시해 터치를 차단합니다.

## 설치 및 설정

1. [GitHub Releases](https://github.com/larosel/ReversePortraitGuard/releases)에서 APK를 내려받아 설치합니다.
2. 앱에서 **다른 앱 위에 표시 권한 설정**을 열어 권한을 허용합니다.
3. 앱에서 **잠금화면 접근성 서비스 설정**을 열어 `Reverse Portrait Guard`를 활성화합니다.
4. 일반 앱 화면도 보호하려면 앱에서 **터치 방지 시작**을 누릅니다.

외부 APK의 접근성 설정이 제한된 경우 앱 정보 화면의 우측 상단 메뉴에서 **제한된 설정 허용**을 먼저 선택해야 할 수 있습니다.

## 제한 사항

- 시스템 내비게이션, 전원·볼륨 버튼 및 일부 보안 UI는 차단되지 않을 수 있습니다.
- 제조사 또는 Android 버전의 보안 정책에 따라 잠금화면 접근성 오버레이 동작이 제한될 수 있습니다.
- 접근성 서비스는 화면 내용을 읽거나 수집하지 않습니다.
- 현재 Release APK는 개인 설치용 디버그 키로 서명되어 있습니다.

## 빌드

요구 사항:

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.0

Debug 빌드:

```powershell
.\gradlew.bat assembleDebug
```

Release 빌드:

```powershell
.\gradlew.bat assembleRelease
```

프로젝트에 포함된 로컬 빌드 스크립트를 사용할 수도 있습니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Configuration Release
```
