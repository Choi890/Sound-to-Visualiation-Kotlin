# 청각장애인 소리 시각화 시스템 Android

웹 프로토타입을 Kotlin/Android 네이티브 앱으로 옮긴 프로젝트입니다. Jetpack Compose UI, MediaPipe Audio Classifier, YAMNet TFLite 모델을 사용합니다.

## 실행

Android Studio에서 이 폴더를 열거나 PowerShell에서 실행합니다.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

디버그 APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 포함 기능

- 실시간 마이크 입력
- YAMNet 기반 초인종/사이렌 감지
- Android SpeechRecognizer 기반 한국어 이름 호출 감지
- 초인종: 파란 원 파동
- 사이렌: 붉은 경고
- 이름 호출: 방향 화살표
- 스테레오 마이크 입력 가능 시 좌/정면/우 방향 추정
- 단일 마이크 기기에서는 방향 표시가 제한됨
- 진동 피드백
- 이벤트 기록: 최근 감지 시간, 방향, 신뢰도 표시
- 민감도 조절: 장소별 소음 수준에 맞춰 초인종/사이렌 감지 임계값 조정
- 다중 호출 이름: 쉼표로 여러 이름 등록
- 화면 플래시, 진동, 화면 켜짐 유지 토글
- 더 선명한 상단 상태판과 색상별 알림 패널
- 대화 실시간 자동 번역: API 없이 ML Kit 온디바이스 번역 모델만 사용
- 로컬 번역 품질 보정: 음성 인식 텍스트 정리, URL/숫자/이메일 보호, 긴 문장 분할, 결과 후처리, 로컬 캐시
- 번역문 음성 출력: 번역 결과를 누르거나 스피커 버튼을 누르면 앱에 포함된 Sherpa-ONNX + Supertonic 3 로컬 TTS로 읽기
- 음성 출력 스타일: 여성/남성 목소리 선택, 내장 엔진 미지원 언어는 Android 시스템 TTS로 대체
- 고품질 내장 TTS 엔진: 별도 TTS 앱 설치 없이 앱 다운로드에 모델과 런타임 포함
- 지원 번역 언어: 한국어, 영어, 일본어, 중국어, 스페인어, 베트남어

## 모델

`app/src/main/assets/yamnet.tflite`에 웹 버전에서 받은 MediaPipe YAMNet 모델을 복사했습니다.

번역은 `com.google.mlkit:translate` 온디바이스 모델만 사용합니다. OpenAI, DeepL, 기타 외부 번역 API 호출 코드는 제거했습니다. 앱에는 API 키 설정이 없고, 번역 요청 텍스트를 외부 번역 서버로 보내지 않습니다.

ML Kit 번역 모델은 APK에 직접 포함하지 않고, 선택한 언어쌍에 맞춰 기기에서 확보됩니다. 이 모델 확보 단계에서는 Google ML Kit 모델 다운로드가 필요할 수 있지만, 실제 번역 추론은 기기 안에서 수행됩니다. 한 번 준비된 언어 모델은 이후 기기 안에서 로컬 번역에 재사용됩니다.

번역문 읽기는 기본적으로 앱에 포함된 Sherpa-ONNX Android 런타임과 Supertonic 3 int8 ONNX 모델을 사용합니다.

- 런타임: `app/libs/sherpa-onnx-1.13.2.aar`
- 모델: `app/src/main/assets/sherpa-onnx-supertonic-3-tts-int8-2026-05-11/`

이 모델은 앱 패키지에 같이 들어가므로 사용자가 별도 TTS 엔진 APK를 설치할 필요가 없습니다. 번역 화면의 `고품질 엔진` 설치 버튼도 제거했습니다. Supertonic 3가 지원하지 않는 언어에서는 Android `TextToSpeech`를 보조 경로로 사용합니다.

출시 패키지는 실제 Android 폰 배포를 기준으로 `arm64-v8a`, `armeabi-v7a` ABI만 포함합니다. x86/x86_64 에뮬레이터까지 지원하려면 `app/build.gradle.kts`의 `ndk.abiFilters`를 조정해야 하지만, 앱 용량이 크게 늘어납니다.

## 출시 전 작업

- Play Store용 keystore 생성
- `app/build.gradle.kts`의 release signingConfig 연결
- 실제 Android 기기에서 마이크 권한, SpeechRecognizer, 방향 추정 검증
- 사이렌/초인종 임계값을 수집 데이터로 보정
- 이름 호출은 출시용으로 온디바이스 키워드 스팟팅 모델을 추가하는 것이 안전함
