# KothaPad 🎙️✍️

**KothaPad** is a speech-enabled notepad for power users built in 100% Kotlin with Jetpack Compose. It features continuous non-stop dictation with an untrusted-silence supervisor, an in-app custom keyboard, one-tap dynamic text stamps, and local-first crash-resilient notes.

---

## 🌟 Key Features

### 1. Robust Continuous Dictation Engine
- **Supervisor Loop**: Treats Android `SpeechRecognizer`'s internal silence timeouts as untrusted; restarts listening on the main thread with near-zero gap.
- **Zero Lost Words**: Partial results are buffered and committed on pause, error, or restart boundaries.
- **Audio Stream Beep Muting**: Mutes `STREAM_SYSTEM` and `STREAM_NOTIFICATION` during continuous session restarts and restores volume seamlessly.
- **Foreground Service**: Microphone foreground service (`DictationService`) with Pause, Resume, and Stop notification actions so dictation keeps running with the screen off.
- **On-Device / Offline Mode**: Uses `createOnDeviceSpeechRecognizer` on API 33+ when offline mode is preferred.
- **Audio Routing**: Real-time detection of Phone Mic, Bluetooth Headsets (SCO), and Wired Headsets.
- **Multi-Mode Support**: Continuous, Toggle, Push-to-Talk (press-and-hold), Single-Shot, and Timed (1-60 min).

### 2. In-App Custom Compose Keyboard
- Built-in QWERTY, Bangla (বাংলা), Symbols, and Bengali Numerals layouts.
- Live cursor navigation buttons (◀ ▶), backspace, enter, space, and layout toggles.
- Works concurrently with speech dictation—type, edit, and move the cursor while dictation continues.
- Toolbar toggle switches between the in-app keyboard and Android's system IME.

### 3. One-Tap Text Stamps & Processing Pipeline
- Horizontally scrollable stamp bar with dynamic token resolution: `{date}`, `{time}`, `{datetime}`, `{cursor}`.
- Three-stage text pipeline:
  1. Spoken Commands & Punctuation (e.g. English "full stop" → ".", Bangla "দাঁড়ি" → "।", "delete last word", "undo").
  2. Spacing cleanups.
  3. Automatic capitalization after sentence terminators.

### 4. Local-First Notes Architecture
- **Room Database**: Notes, Stamps, and Spoken Commands entities.
- **DataStore Preferences**: Typed preferences for dictation modes, pause tolerance, themes, and font styling.
- **Debounced Auto-Save (500ms)** and lifecycle flushes on `onPause`/`onStop`.
- **In-Memory Undo/Redo**: Multi-step undo/redo stack.
- **Privacy First**: 0 accounts, 0 ads, 0 network telemetry.

### 5. Visual Identity & Adaptive Icon
- Warm AMOLED Black palette with vibrant Saffron/Burnt-Orange accents (`#FF7043`).
- Bundled offline Google Fonts: Noto Sans Bengali, Inter, JetBrains Mono.
- Custom adaptive icon featuring a calligraphic pen nib, audio soundwave, and text cursor, with an Android 13+ monochrome layer.

---

## 🛠️ Build & Run Instructions

```bash
# Build debug APK (automatically places KothaPad.apk in root directory)
gradle assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest
```

---

## 🔄 GitHub Sync & Cross-Account Importing

When you push this repository to GitHub and re-import it into another Google AI Studio account or machine:

### Why Certain Files Are Excluded in Git
- **Compiled APKs & `build/` folder**: Git ignores build output directories (`app/build/`) to keep commit history fast and clean.
- **Environment Secrets (`.env`)**: Git ignores `.env` to prevent private credentials from leaking.
- **Signing Keystores (`debug.keystore`)**: Binary keystores are gitignored; instead, `debug.keystore.base64` is tracked in Git.

### Automated Self-Healing
The workspace is configured to self-heal on import:
1. **Gradle Pre-flight Auto-Restore**: When Gradle runs any build task, `app/build.gradle.kts` automatically checks for `debug.keystore` and `.env`. If missing, it automatically decodes `debug.keystore.base64` and generates `.env` from `.env.example`.
2. **Auto-Copy APK**: Every `./gradlew assembleDebug` run automatically copies the output APK to `KothaPad.apk` in the root workspace folder for easy download.
3. **One-Command Setup**: You can also run the bundled setup script:
   ```bash
   ./setup_workspace.sh
   ```
