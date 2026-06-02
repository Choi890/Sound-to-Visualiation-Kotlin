# Sound to Visualiation Kotlin 프로젝트 구조 설명

## 프로젝트 한줄 설명

Android Kotlin 접근성/소리 시각화 앱입니다. 주변 소리 감지, 음성 인식, 번역 모델, 알림 UI를 활용해 소리를 시각적 정보로 바꾸는 프로젝트입니다.

## 기본 작동 흐름

- MainActivity와 서비스가 마이크/모델/화면 상태를 연결합니다.
- SoundDetectionController, SoundFeatureExtractor, SpeechTextInferencer가 소리 특징 추출과 모델 추론을 수행합니다.
- assets의 ONNX/TFLite/Bergamot 모델과 번역 리소스가 로컬 음성/소리/번역 기능을 지원합니다.

## 문서 기준

- 아래 목록은 `git ls-files`로 확인되는 Git 추적 파일을 기준으로 작성했습니다.
- `.git`, `node_modules`, `build`, `.gradle`, 임시 업로드/출력물처럼 Git이 관리하지 않는 폴더는 제외했습니다.
- 폴더 표는 코드와 자산이 어떤 책임으로 나뉘는지, 파일 표는 각 파일이 실제로 무엇을 담당하는지 설명합니다.

## 폴더별 설명 (27개)

| 폴더 | 설명 |
| --- | --- |
| `.` | 프로젝트 루트입니다. 실행/빌드 설정, README, 전체 구조 문서, 최상위 진입 파일이 모여 있습니다. |
| `app` | Android 앱 모듈입니다. 앱 전용 빌드 설정, 소스 코드, 리소스, ProGuard 설정이 이 아래에 있습니다. |
| `app/libs` | Android 빌드에 직접 포함하는 로컬 AAR/JAR 라이브러리를 보관합니다. |
| `app/src` | Android 소스 세트가 들어 있는 상위 폴더입니다. main, test 같은 빌드 대상별 파일을 구분합니다. |
| `app/src/main` | 실제 앱에 포함되는 AndroidManifest, Kotlin/Java 소스, 리소스, 에셋을 담는 기본 소스 세트입니다. |
| `app/src/main/assets` | APK 안에 원본 그대로 포함되는 파일 자산 폴더입니다. 모델, 샘플 오디오, 라이선스, 웹 브리지 파일 등이 이곳에 들어갑니다. |
| `app/src/main/assets/bergamot` | 로컬 번역 기능에 필요한 Bergamot 웹 브리지, 워커, 번역 모델 파일을 보관합니다. |
| `app/src/main/assets/bergamot/models` | 로컬 번역 기능에 필요한 Bergamot 웹 브리지, 워커, 번역 모델 파일을 보관합니다. |
| `app/src/main/assets/bergamot/models/enko` | 로컬 번역 기능에 필요한 Bergamot 웹 브리지, 워커, 번역 모델 파일을 보관합니다. |
| `app/src/main/assets/bergamot/models/jaen` | 로컬 번역 기능에 필요한 Bergamot 웹 브리지, 워커, 번역 모델 파일을 보관합니다. |
| `app/src/main/assets/bergamot/models/koen` | 로컬 번역 기능에 필요한 Bergamot 웹 브리지, 워커, 번역 모델 파일을 보관합니다. |
| `app/src/main/assets/bergamot/worker` | 로컬 번역 기능에 필요한 Bergamot 웹 브리지, 워커, 번역 모델 파일을 보관합니다. |
| `app/src/main/assets/licenses` | 앱에 포함된 모델, 폰트, 데이터셋의 라이선스 고지 파일을 보관합니다. |
| `app/src/main/assets/sherpa-onnx-sense-voice-ko-en-ja-int8-2024-07-17` | Sherpa-ONNX 음성 인식 모델과 토큰/라이선스 파일을 보관합니다. |
| `app/src/main/java` | 앱의 Kotlin/Java 패키지 루트입니다. 패키지명에 맞춰 실제 클래스 파일이 하위 폴더에 배치됩니다. |
| `app/src/main/java/com` | Kotlin 패키지 네임스페이스의 `com` 단계입니다. 실제 앱 패키지는 이 아래 `findmine`, `focussound`, `ownlifeos` 같은 이름으로 이어집니다. |
| `app/src/main/java/com/soundvisualization` | Sound to Visualiation Kotlin 앱의 최상위 Kotlin 패키지입니다. 화면 진입점과 주요 기능 패키지가 이 아래에서 갈라집니다. |
| `app/src/main/java/com/soundvisualization/accessibility` | 소리 감지, 음성 인식, 번역, 알림 UI 등 접근성 기능의 핵심 Kotlin 패키지입니다. |
| `app/src/main/res` | Android XML 리소스 루트입니다. 문자열, 색상, 스타일, 아이콘, XML 설정처럼 코드가 참조하는 리소스를 보관합니다. |
| `app/src/main/res/drawable` | Android 벡터/드로어블 이미지 리소스 폴더입니다. 아이콘이나 그래픽 XML을 보관합니다. |
| `app/src/main/res/font` | font 관련 파일을 기능별로 묶어 둔 폴더입니다. 같은 책임의 코드나 자산을 한 위치에서 관리하기 위해 사용합니다. |
| `app/src/main/res/mipmap-anydpi-v26` | Android 런처 아이콘처럼 해상도별 앱 아이콘 리소스를 보관합니다. |
| `app/src/main/res/values` | 문자열, 색상, 테마, 스타일 등 앱 전역 XML 값을 정의하는 리소스 폴더입니다. |
| `app/src/main/res/xml` | 백업 규칙, 파일 공유 경로, 데이터 추출 규칙처럼 Android 시스템에 전달하는 XML 설정을 보관합니다. |
| `gradle` | Gradle Wrapper와 데몬 설정처럼 Android/Kotlin 빌드 도구가 사용하는 파일을 보관합니다. |
| `gradle/wrapper` | 개발 PC에 Gradle이 없어도 동일한 버전으로 빌드할 수 있게 하는 Wrapper 실행 파일과 속성 파일을 보관합니다. |
| `tools` | tools 관련 파일을 기능별로 묶어 둔 폴더입니다. 같은 책임의 코드나 자산을 한 위치에서 관리하기 위해 사용합니다. |

## 파일별 설명 (297개)

| 파일 | 설명 |
| --- | --- |
| `.gitattributes` | Git이 줄바꿈, 바이너리 파일, 대용량 파일 속성을 어떻게 다룰지 지정하는 저장소 속성 파일입니다. |
| `.gitignore` | Git에 올리지 않을 빌드 산출물, 캐시, 개인 환경 파일을 지정하는 설정 파일입니다. 저장소에는 필요한 소스/자산만 남기도록 도와줍니다. |
| `after_close.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `after_feedback.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `alarm_hidden_backdrop.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `alarm_local_blur.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `alarm_strong_backdrop.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `alarm_test_popup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `alert_blur_no_buttons.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `alert_icon_buttons.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `alert_icon_buttons_device.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `alert_no_labels.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `always_on_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `app/build.gradle.kts` | Android 앱 모듈의 Gradle 빌드 설정입니다. SDK 버전, 의존성, Kotlin/Compose/Room 같은 모듈별 빌드 옵션을 지정합니다. |
| `app/libs/sherpa-onnx-1.13.2.aar` | Android 앱에 직접 포함되는 AAR 라이브러리입니다. Gradle 의존성으로 연결되어 네이티브/Java API를 제공합니다. |
| `app/proguard-rules.pro` | 릴리스 빌드에서 코드 축소/난독화를 할 때 유지해야 할 클래스나 예외 규칙을 지정합니다. |
| `app/src/main/AndroidManifest.xml` | Android 앱의 패키지 구성, Activity/Service, 권한, 파일 provider 같은 시스템 등록 정보를 선언합니다. |
| `app/src/main/assets/asr_sources.json` | 앱에 포함된 모델/리소스의 출처, 파일명, 언어쌍, 로딩 정보를 기록하는 JSON 메타데이터 파일입니다. |
| `app/src/main/assets/bergamot/bridge.html` | Android WebView에서 번역 JavaScript와 워커를 로드하기 위한 브리지 HTML 파일입니다. |
| `app/src/main/assets/bergamot/bridge.js` | Bergamot 번역 엔진을 WebView/워커 환경에서 초기화하고 메시지를 주고받는 JavaScript 브리지 코드입니다. |
| `app/src/main/assets/bergamot/models/enko/lex.50.50.enko.s2t.bin` | Bergamot 로컬 번역 모델의 바이너리 데이터입니다. 언어쌍별 번역 추론에 로드됩니다. |
| `app/src/main/assets/bergamot/models/enko/model.enko.intgemm.alphas.bin` | Bergamot 로컬 번역 모델의 바이너리 데이터입니다. 언어쌍별 번역 추론에 로드됩니다. |
| `app/src/main/assets/bergamot/models/enko/vocab.enko.spm` | Bergamot 번역 모델이 문장을 토큰으로 나누고 복원할 때 사용하는 SentencePiece 어휘 모델입니다. |
| `app/src/main/assets/bergamot/models/jaen/lex.50.50.jaen.s2t.bin` | Bergamot 로컬 번역 모델의 바이너리 데이터입니다. 언어쌍별 번역 추론에 로드됩니다. |
| `app/src/main/assets/bergamot/models/jaen/model.jaen.intgemm.alphas.bin` | Bergamot 로컬 번역 모델의 바이너리 데이터입니다. 언어쌍별 번역 추론에 로드됩니다. |
| `app/src/main/assets/bergamot/models/jaen/vocab.jaen.spm` | Bergamot 번역 모델이 문장을 토큰으로 나누고 복원할 때 사용하는 SentencePiece 어휘 모델입니다. |
| `app/src/main/assets/bergamot/models/koen/lex.50.50.koen.s2t.bin` | Bergamot 로컬 번역 모델의 바이너리 데이터입니다. 언어쌍별 번역 추론에 로드됩니다. |
| `app/src/main/assets/bergamot/models/koen/model.koen.intgemm.alphas.bin` | Bergamot 로컬 번역 모델의 바이너리 데이터입니다. 언어쌍별 번역 추론에 로드됩니다. |
| `app/src/main/assets/bergamot/models/koen/vocab.koen.spm` | Bergamot 번역 모델이 문장을 토큰으로 나누고 복원할 때 사용하는 SentencePiece 어휘 모델입니다. |
| `app/src/main/assets/bergamot/models/registry.json` | 앱에 포함된 모델/리소스의 출처, 파일명, 언어쌍, 로딩 정보를 기록하는 JSON 메타데이터 파일입니다. |
| `app/src/main/assets/bergamot/models/sources.json` | 앱에 포함된 모델/리소스의 출처, 파일명, 언어쌍, 로딩 정보를 기록하는 JSON 메타데이터 파일입니다. |
| `app/src/main/assets/bergamot/translator.js` | Bergamot 번역 엔진을 WebView/워커 환경에서 초기화하고 메시지를 주고받는 JavaScript 브리지 코드입니다. |
| `app/src/main/assets/bergamot/worker/bergamot-translator-worker.js` | Bergamot 번역 엔진을 WebView/워커 환경에서 초기화하고 메시지를 주고받는 JavaScript 브리지 코드입니다. |
| `app/src/main/assets/bergamot/worker/bergamot-translator-worker.wasm` | 브라우저/WebView 안에서 Bergamot 번역 연산을 수행하는 WebAssembly 바이너리입니다. |
| `app/src/main/assets/bergamot/worker/translator-worker.js` | Bergamot 번역 엔진을 WebView/워커 환경에서 초기화하고 메시지를 주고받는 JavaScript 브리지 코드입니다. |
| `app/src/main/assets/licenses/pretendard_ofl.txt` | 앱에 포함된 외부 폰트, 모델, 데이터의 저작권/라이선스 고지 파일입니다. |
| `app/src/main/assets/licenses/yamnet_class_map_apache_2.txt` | 앱에 포함된 외부 폰트, 모델, 데이터의 저작권/라이선스 고지 파일입니다. |
| `app/src/main/assets/sherpa-onnx-sense-voice-ko-en-ja-int8-2024-07-17/LICENSE` | Sherpa-ONNX SenseVoice 음성 인식 모델을 앱에 포함할 때 필요한 라이선스 고지 파일입니다. |
| `app/src/main/assets/sherpa-onnx-sense-voice-ko-en-ja-int8-2024-07-17/model.int8.onnx` | Sherpa-ONNX 기반 음성 인식 모델 가중치입니다. 로컬 음성-텍스트 변환 추론에 사용됩니다. |
| `app/src/main/assets/sherpa-onnx-sense-voice-ko-en-ja-int8-2024-07-17/README.md` | 프로젝트 개요, 실행 방법, 주요 기능을 설명하는 기본 안내 문서입니다. |
| `app/src/main/assets/sherpa-onnx-sense-voice-ko-en-ja-int8-2024-07-17/tokens.txt` | 음성 인식 모델이 출력 토큰을 문자/단어로 복원할 때 사용하는 토큰 사전입니다. |
| `app/src/main/assets/yamnet.tflite` | TensorFlow Lite 모델 파일입니다. Android에서 주변 소리 분류나 특징 추론에 사용됩니다. |
| `app/src/main/assets/yamnet_class_map.csv` | 모델 출력 인덱스와 사람이 읽을 수 있는 라벨을 연결하는 CSV 데이터 파일입니다. |
| `app/src/main/baseline-prof.txt` | Android Baseline Profile 설정 파일입니다. 자주 실행되는 코드 경로를 미리 알려 앱 시작과 주요 화면 성능을 개선하는 데 사용합니다. |
| `app/src/main/java/com/soundvisualization/accessibility/AppLocalization.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. AppLocalization Kotlin 소스입니다. 주 역할은 앱 화면 문구와 언어별 표시 값을 관리해 로컬라이징을 지원 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/ConversationTranslator.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. ConversationTranslator Kotlin 소스입니다. 주 역할은 인식된 대화 문장을 로컬 번역 모델로 번역하고 화면 표시용 결과로 변환 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/LocalAppStore.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. LocalAppStore Kotlin 소스입니다. 주 역할은 앱 내부 설정과 사용자 상태를 로컬에 저장하고 다시 읽는 저장소 역할 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/MainActivity.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. MainActivity Kotlin 소스입니다. 주 역할은 Android 화면 진입점과 UI 초기화 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/NameCallRecognizer.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. NameCallRecognizer Kotlin 소스입니다. 주 역할은 음성/이름/패턴 인식 처리 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/SoundDetectionController.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. SoundDetectionController Kotlin 소스입니다. 주 역할은 기능 실행 흐름을 조율하고 상태 전환을 관리 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/SoundFeatureExtractor.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. SoundFeatureExtractor Kotlin 소스입니다. 주 역할은 마이크 입력에서 주파수/세기/특징 벡터를 추출해 소리 분류 모델 입력으로 변환 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/SoundModels.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. SoundModels Kotlin 소스입니다. 주 역할은 도메인 또는 UI에서 쓰는 데이터 모델 정의 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/SoundMonitorService.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. SoundMonitorService Kotlin 소스입니다. 주 역할은 백그라운드에서 계속 동작하는 Android 서비스 로직 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/SoundTrainingRecorder.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. SoundTrainingRecorder Kotlin 소스입니다. 주 역할은 학습 또는 분석에 필요한 소리 입력 녹음 처리 입니다. |
| `app/src/main/java/com/soundvisualization/accessibility/SpeechTextInferencer.kt` | Sound to Visualiation Kotlin의 접근성 기능 코드로, 소리 감지/음성 인식/번역/알림 화면을 담당합니다. SpeechTextInferencer Kotlin 소스입니다. 주 역할은 모델 추론을 실행하고 결과를 앱 데이터로 변환 입니다. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Android 적응형 런처 아이콘의 전경 그래픽을 정의합니다. |
| `app/src/main/res/font/pretendard_variable.ttf` | Pretendard Variable 폰트 파일입니다. 앱 화면의 한글/영문 텍스트를 지정한 글꼴로 렌더링할 때 사용합니다. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Android 런처 아이콘 또는 적응형 아이콘 구성을 정의하는 XML 리소스입니다. |
| `app/src/main/res/values/colors.xml` | 앱에서 사용하는 색상 리소스를 이름으로 정의합니다. |
| `app/src/main/res/values/strings.xml` | 앱에서 표시하는 문자열 리소스를 한 곳에 모아 다국어 처리와 재사용을 쉽게 합니다. |
| `app/src/main/res/values/styles.xml` | 앱 테마와 공통 스타일을 정의해 화면 전반의 색상/폰트/컴포넌트 모양을 통일합니다. |
| `app/src/main/res/xml/backup_rules.xml` | Android 자동 백업에 포함하거나 제외할 앱 데이터를 지정하는 XML 규칙입니다. |
| `app/src/main/res/xml/data_extraction_rules.xml` | Android 데이터 추출/백업 정책에서 어떤 데이터를 이동 가능한지 지정하는 XML 규칙입니다. |
| `app-current.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `bottom_bar_after.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `bottom_bar_after_taps.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `bottom_bar_mid.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `bottom_drag_after.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `bottom_drag_back.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `bottom_drag_linked.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `bottom_glow_fix.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `bottom_outline_glow.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `build.gradle.kts` | 루트 Gradle 빌드 설정입니다. Android/Kotlin 플러그인과 전체 프로젝트 빌드 구성을 정의합니다. |
| `calibration_popup_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `card_origin_popup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `center_alarm_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `center_alarm_clean.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `center_alarm_final.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `center_alarm_fixed.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `content_burst_popup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `current_screen.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `current_ui.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `current_ui_review.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `dark_dashboard_redesign.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `dark_ui_after.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `edge_glass_final.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `edge_glass_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `edge_glass_manage.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `english_toggle_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `exchange_redesign_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `exchange_redesign_manage.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `exchange_redesign_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `final_korean.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `final_ui.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `final_window.xml` | 접근성/레이아웃 검수용 최종 화면 UI 계층 덤프 XML입니다. 실제 화면 구조와 노드 정보를 확인할 때 사용합니다. |
| `framestats.txt` | Android GPU 렌더링/프레임 시간 측정 결과를 기록한 성능 분석 텍스트 파일입니다. |
| `framestats_stay.txt` | 특정 화면에 머무는 동안 수집한 Android 프레임 시간 측정 결과입니다. UI 성능 비교에 사용합니다. |
| `gradle.properties` | Gradle 빌드 성능, AndroidX 사용 여부, Kotlin/빌드 옵션 같은 공통 속성을 지정합니다. |
| `gradle/gradle-daemon-jvm.properties` | Gradle 데몬이 사용할 JVM 관련 속성을 지정해 빌드 환경을 일정하게 유지합니다. |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle Wrapper가 지정된 Gradle 버전을 내려받고 실행하는 데 사용하는 바이너리 파일입니다. |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper가 사용할 Gradle 배포판 버전과 다운로드 URL을 지정합니다. |
| `gradlew` | Unix/macOS/Linux에서 Gradle Wrapper를 실행하는 스크립트입니다. |
| `gradlew.bat` | Windows에서 Gradle Wrapper를 실행하는 배치 스크립트입니다. |
| `hamburger_animation_before.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `hamburger_animation_closed.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `hamburger_animation_open.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `hamburger_animation_open2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `hamburger_animation_scrolled.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `hamburger_popup_blur_fade.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `hamburger_popup_blur_fade_open.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `hamburger_popup_blur_fade_open_actual.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `hamburger_popup_blur_fade_open_real.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `history_popup_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `improved_window.xml` | 개선 후 화면의 UI 계층 덤프 XML입니다. 변경 전후 접근성/레이아웃 구조 비교에 사용합니다. |
| `layout_deck.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `layout_deck_final.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `layout_deck_fixed.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `liquid_popup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `manage_cards.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `manage_cards_2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `manage_cards_3.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `manage_cards_4.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `manage_cards_5.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `manage_dialog.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `manage_screen.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `manage_scrolled.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `no_top_toggle_no_center_action.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `observatory_dark.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `observatory_light.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `oura_final.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `oura_redesign.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `popup_blur_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `popup_calibration.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `popup_history.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `popup_inner_stays.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `popup_outside_closes.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `premium_alarm_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `profile_screen.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `profile_screen2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `profile_screen3.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `PROJECT_STRUCTURE.md` | 프로젝트의 모든 주요 폴더와 Git 추적 파일을 한글로 설명하는 구조 문서입니다. 처음 보는 사람이 경로별 역할을 빠르게 파악하기 위해 추가했습니다. |
| `README.md` | 프로젝트 개요, 실행 방법, 주요 기능을 설명하는 기본 안내 문서입니다. |
| `redesign.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `redesign_dark.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `redesign2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `settings.gradle.kts` | Gradle이 인식할 프로젝트 이름과 포함할 모듈을 지정하는 설정 파일입니다. |
| `sound_card_event_ignored.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `sound_card_volume_only.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `sound_page_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `sound_ui.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `sound_ui_after.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_80band.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_9band_80zone.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_accessible_emergency.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_accessible_management.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_accessible_sound.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_accessible_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_accessible_test_popup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_accuracy_after_tune.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_accuracy_pipeline.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_after_activation.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_after_compact.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_after_stop_detection.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_bottom.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_bottom_adaptive_blur.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_bottom_adaptive_blur_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_bottom_float.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_bottom_shadow_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_capsule_preload_latest.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_capsule_preload_latest_pull.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_capsule_sync.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_compact_sound.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_current_after_preload_fix.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_dark_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_demo_after_fix.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_demo_alarm_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_detection_accuracy_on.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_dotlottie_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_english_after_opt.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_english_latest.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_english_light_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_eq_sensitive.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_final_improved.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_header_caption_lang.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_header_caption_lang_final.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_header_caption_lang_fixed.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_improved_manage.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_improved_sound.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_improved_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_language_toggle_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_latest.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_latest_alarm.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_latest_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_latest_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_latest_light.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_latest_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_light_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_liquid_ui_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_liquid_ui_management.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_liquid_ui_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_local_lottie_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_local_lottie_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_manage_after_layout.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_manage_optimized.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_manage_page.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_manage_synced.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_manage_synced3.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_management_after_cleanup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_management_after_nav.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_nav_recheck.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_new_liquid_manage.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_new_liquid_popup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_new_liquid_popup_open.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_new_liquid_sound.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_new_liquid_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_new_liquid_speech_compact.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_optimized_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_orbit_no_wave_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_page_peek.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_page_peek_middle.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_peek_carousel.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_peek_carousel_middle.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_popup_after_changes.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_popup_bottom_nav.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_redesign_light.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_redesign_light_real.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_redesign_sound.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_redesign_theme_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_refined_demoalert.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_refined_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_refined_manage.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_refined_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_refined_speech2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_refined_testpopup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_refined_testpopup2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_scroll_column_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_smooth_manage.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_smooth_mid.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_speech_manual_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_speech_page.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_speech_pipeline_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_speech_synced.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_speech_synced2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_speech_synced3.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_split_pipeline.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_test_popup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_test_popup_2col.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_test_popup_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_translation_animation_verified.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_translation_fast_language_switch.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_translation_manual_lang.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_translation_manual_lang_changed.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_voice_orbit_final_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_voice_orbit_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_voice_orbit_speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_voice_orbit_speech_on.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_waveform_home.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_waveform_reference.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_waveform_shape.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz_window.xml` | 소리 시각화 화면의 UI 계층 덤프 XML입니다. 화면 노드, 텍스트, 접근성 속성을 검사하는 데 사용합니다. |
| `soundviz-current.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-current-device.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-final.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-management.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-management2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-management-current.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-management-current2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-management-current3.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-popup-current.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-popup-current2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-popup-latest.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-screen.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-screen-fixed.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-speech.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-speech2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-speech-current.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-speech-current2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `soundviz-speech-current3.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `speech_manual_window.xml` | 음성/수동 입력 화면의 UI 계층 덤프 XML입니다. 입력 화면 구조와 접근성 상태를 확인합니다. |
| `speech_page_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `speech_ui_after.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `test_alarm_bar_fixed.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `test_popup.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `test_popup_open.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `test_popup2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `test_popup3.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `tools/profile-gpu-rendering.ps1` | Android GPU 렌더링 프로파일링 명령을 Windows PowerShell에서 실행하기 위한 성능 측정 보조 스크립트입니다. |
| `top_card_synced_transition.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `top_card_transition_check.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `ui_hierarchy_refined.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `ui_management_grouped.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `window.xml` | 기본 화면 상태의 UI 계층 덤프 XML입니다. 디버깅과 접근성 검수 기준 파일로 사용합니다. |
| `window_after_semantics.xml` | semantics 개선 후 화면 노드 구조를 기록한 UI 계층 덤프 XML입니다. |
| `window_en.xml` | 영문 UI 상태의 화면 계층 덤프 XML입니다. 다국어 표시와 접근성 노드 확인에 사용합니다. |
| `window_hamburger.xml` | 햄버거 메뉴를 연 상태의 화면 계층 덤프 XML입니다. 메뉴 구조와 클릭 가능 노드를 확인합니다. |
| `window_ko.xml` | 한국어 UI 상태의 화면 계층 덤프 XML입니다. 한글 표시와 접근성 노드 확인에 사용합니다. |
| `window_speech_ko.xml` | 한국어 음성 화면 상태의 UI 계층 덤프 XML입니다. 음성 관련 화면 구조 검수에 사용합니다. |

## 읽는 방법

- 먼저 폴더별 설명에서 큰 기능 묶음을 확인한 다음, 파일별 설명에서 실제 구현 파일을 찾으면 됩니다.
- Android 프로젝트는 `app/src/main/java` 아래 Kotlin 파일이 핵심 코드이고, `app/src/main/res`와 `app/src/main/assets`는 화면/모델/오디오 자산입니다.
- 웹 프로젝트는 `index.html`, `styles.css`, `script.js` 또는 `app.js`가 화면 구조, 스타일, 동작을 나눠 담당합니다.
- Python 프로젝트는 루트의 실행 스크립트와 `src`, `backend`, `scripts`, `tests` 폴더를 함께 보면 처리 흐름을 이해할 수 있습니다.
