# LeoDisplay Android TV Player v1.0.0

Aplicação Android TV nativa para o ecossistema LeoDisplay Digital Signage.

---

## Stack

- **Android Studio** Hedgehog (2023.1.1) ou superior
- **Kotlin** 1.9.22
- **Gradle** 8.2
- **minSdk** 24 (Android 7.0)
- **targetSdk** 34 (Android 14)
- **compileSdk** 34

---

## Estrutura do Projeto

```
app/src/main/java/com/leodisplay/player/
├── config/
│   └── Config.kt              # Constantes de configuração (URL, timers, etc.)
├── network/
│   └── NetworkMonitor.kt      # Monitorização de conectividade
├── storage/
│   └── StorageManager.kt      # DataStore Preferences (preparado para Sprint 2)
├── utils/
│   ├── Utils.kt               # Funções utilitárias (fullscreen, screen on)
│   └── WebViewHelper.kt       # Configuração centralizada do WebView
├── ui/
│   ├── splash/
│   │   └── SplashActivity.kt  # Ecrã inicial (2 segundos)
│   ├── player/
│   │   └── PlayerActivity.kt  # WebView com o player web
│   └── error/
│       └── ErrorActivity.kt   # Ecrã de erro com auto-reconnect
```

---

## Funcionalidades Implementadas (Sprint APK 1)

| Funcionalidade | Estado |
|----------------|--------|
| Splash Screen com logo | ✅ |
| WebView otimizado para Digital Signage | ✅ |
| Fullscreen sem barras de navegação | ✅ |
| Keep screen on / Impedir sleep | ✅ |
| Modo Landscape obrigatório | ✅ |
| JavaScript, DOM Storage, Cache | ✅ |
| Media playback sem interação | ✅ |
| Hardware Acceleration | ✅ |
| User Agent personalizado | ✅ |
| Erro de ligação com retry | ✅ |
| Auto-reconnect a cada 10 segundos | ✅ |
| Navegação por comando remoto (D-pad) | ✅ |
| Compatível Android TV / Google TV | ✅ |
| Config.kt centralizado | ✅ |
| MVVM-ready (Lifecycle, ViewModel) | ✅ |
| DataStore preparado | ✅ |
| Código comentado | ✅ |

---

## NÃO Implementado (Sprint APK 2)

- Device Registration
- QR Code
- Supabase Login
- Heartbeat
- Auto Boot
- OTA Updates
- Kiosk Lock

---

## Como Compilar

### 1. Abrir no Android Studio

```bash
# Clonar ou extrair o projeto
File → Open → selecionar pasta leodisplay-android
```

### 2. Sincronizar Gradle

```bash
# Android Studio faz automaticamente, ou:
./gradlew sync
```

### 3. Configurar URL do Servidor

Editar `app/src/main/java/com/leodisplay/player/config/Config.kt`:

```kotlin
const val SERVER_URL = "https://leodisplay.netlify.app"
```

### 4. Gerar APK Debug

```bash
./gradlew assembleDebug
```

**Output:** `app/build/outputs/apk/debug/app-debug.apk`

### 5. Gerar APK Release

```bash
./gradlew assembleRelease
```

**Output:** `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## Como Testar numa Android TV

### Método 1: ADB via Wi-Fi

```bash
# 1. Ativar Developer Options na TV
#    Settings → About → Build (clicar 7 vezes)

# 2. Ativar ADB e Debugging via Wi-Fi
#    Settings → Developer Options → ADB Debugging → ON
#    Settings → Developer Options → Debug over Wi-Fi → ON

# 3. Verificar IP da TV
#    Settings → Network → IP Address (ex: 192.168.1.50)

# 4. Conectar via ADB
adb connect 192.168.1.50:5555

# 5. Instalar APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 6. Lançar app
adb shell am start -n com.leodisplay.player/.ui.splash.SplashActivity
```

### Método 2: ADB via USB

```bash
# 1. Ligar TV ao PC via cabo USB
# 2. Verificar dispositivo
adb devices

# 3. Instalar
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Método 3: Android Studio (mais fácil)

```bash
# 1. Conectar TV (Wi-Fi ou USB)
# 2. No Android Studio: Run → Select Device → escolher a TV
# 3. Click em Run (Shift+F10)
```

---

## Comandos ADB Úteis

```bash
# Ver logs em tempo real
adb logcat -s LeoDisplay:D

# Desinstalar app
adb uninstall com.leodisplay.player

# Forçar paragem
adb shell am force-stop com.leodisplay.player

# Limpar dados
adb shell pm clear com.leodisplay.player

# Capturar screenshot
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Gravar vídeo do ecrã (3 min max)
adb shell screenrecord /sdcard/video.mp4
adb pull /sdcard/video.mp4
```

---

## Personalização

### Alterar Logo

Substituir `app/src/main/res/drawable/ic_logo.xml` pelo vector asset do LeoDisplay.

### Alterar Cores

Editar `app/src/main/res/values/colors.xml`.

### Alterar Duração do Splash

Editar `Config.kt`:
```kotlin
const val SPLASH_DURATION_MS = 2000L  // 2 segundos
```

### Alterar Intervalo de Reconnect

Editar `Config.kt`:
```kotlin
const val RECONNECT_INTERVAL_MS = 10000L  // 10 segundos
```

---

## Licença

Proprietário — LeoDisplay / Parque Aquático de Amarante
