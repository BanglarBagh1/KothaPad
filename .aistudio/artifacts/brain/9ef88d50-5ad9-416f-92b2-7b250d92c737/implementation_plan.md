# Cross-Account Workspace Sync & Auto-Restore Plan

Ensure seamless repository syncing between Google AI Studio and GitHub so that imported projects in other accounts or workspaces automatically restore environment credentials, keystores, and compiled APKs without manual troubleshooting.

## User Review & Critical Decisions

> [!IMPORTANT]
> The confirmed preferences from Phase 1 have been incorporated into this architecture:

- **Confirmed Decision 1**: Rebuild APK automatically on workspace import rather than bloating git history with large binary commits.
- **Confirmed Decision 2**: Provide an automated bootstrap mechanism to reconstruct `debug.keystore` and `.env` files immediately when cloned into a new workspace.

---

## 1. Overview & Core Concept

### Why Files Are Missing When Syncing to GitHub
When an AI Studio workspace is pushed to GitHub, Git intentionally filters files matching `.gitignore` rules:
1. **Build Outputs (`app/build/` and `.apk`)**: Android APKs are compiled binaries (~18MB). Git ignores the `build/` directory by default to keep commit history lightweight and avoid merge conflicts.
2. **Environment Variables (`.env`)**: Secret tokens and private API keys are gitignored to prevent security exposure in public or shared repositories.
3. **Keystores (`debug.keystore`)**: Binary signing keys are gitignored by standard Android rules. The repository retains `debug.keystore.base64` as a portable encoded source.

### The Solution
Instead of fighting Git conventions with heavy binary commits, we configure self-healing automation:
- **Automatic Gradle Pre-flight Task**: A lightweight Gradle lifecycle hook in `build.gradle.kts` that detects if `debug.keystore` or `.env` are missing on build, automatically decoding the base64 keystore and copying `.env.example` to `.env`.
- **Root Post-Build APK Sync**: An automated task ensuring that every debug build automatically outputs `KothaPad.apk` in the workspace root directory for easy download.
- **One-Command Shell Script (`setup_workspace.sh`)**: A portable script that sets up keystores, environment files, and builds the APK in one command.

---

## 2. User Experience & Workflow

### User Flow in a New AI Studio Account / Workspace
```
┌─────────────────────────────────┐
│ Import Repo into New AI Studio  │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ First Build / compile_applet    │
│ (Gradle automatically restores  │
│  debug.keystore & .env)         │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ Build Succeeds & Places         │
│ KothaPad.apk at Workspace Root  │
└─────────────────────────────────┘
```

1. **Importing into AI Studio**: User clones or imports the repository into any new account.
2. **First Run**: AI Studio triggers `compile_applet` or the user runs `./setup_workspace.sh`.
3. **Automatic Reconstruction**: Pre-build tasks decode `debug.keystore.base64` into `debug.keystore` and instantiate `.env` from `.env.example`.
4. **Immediate Output**: The build outputs `KothaPad.apk` and `app-debug.apk` directly in the root workspace folder, ready for download.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Automated Gradle Pre-flight vs. Manual Terminal Commands**
  - *Chosen Approach*: Embed the bootstrap check into Gradle's configuration/pre-build phase (`app/build.gradle.kts`).
  - *Why*: In Google AI Studio, new workspaces trigger builds automatically. If the build file itself handles the base64 decoding and `.env` fallback, the build never fails on missing files, even if the user never opens a terminal.
  - *Alternative Considered*: Requiring manual terminal commands (`base64 -d ...`). Rejected because it requires user intervention on every new clone.

- **Decision 2: Automated Root APK Placement**
  - *Chosen Approach*: Add a post-build task (`copyApkToRoot`) tied to `assembleDebug` that copies the APK to `KothaPad.apk` at the root directory.
  - *Why*: Gives the user immediate access in the workspace file tree without having to drill down into `app/build/outputs/apk/debug/`.

---

## 4. Technical Architecture & Implementation Steps

```
Workspace Root
├── .env.example (Checked into Git)
├── debug.keystore.base64 (Checked into Git)
├── setup_workspace.sh (Automated bootstrap script)
├── build.gradle.kts (Lifecycle pre-build auto-decode)
└── app/build.gradle.kts (Auto-copy APK to root task)
```

### Proposed Changes:

1. **Gradle Self-Healing Hook (`build.gradle.kts` / `app/build.gradle.kts`)**:
   - Register a task/hook running before `preBuild`:
     - Checks if `rootDir/debug.keystore` exists; if not, decodes `debug.keystore.base64`.
     - Checks if `rootDir/.env` exists; if not, copies `rootDir/.env.example` to `rootDir/.env`.
   - Register a post-task on `assembleDebug` to copy `app-debug.apk` to `${rootDir}/KothaPad.apk`.

2. **Standalone Bootstrap Script (`setup_workspace.sh`)**:
   - Executable bash script for command-line users or CI environments:
     ```bash
     #!/usr/bin/env bash
     set -e
     [ ! -f debug.keystore ] && base64 -d debug.keystore.base64 > debug.keystore
     [ ! -f .env ] && cp .env.example .env
     ./gradlew assembleDebug
     cp app/build/outputs/apk/debug/app-debug.apk KothaPad.apk
     echo "Workspace ready! KothaPad.apk created."
     ```

3. **Documentation (`README.md`)**:
   - Add a brief "Syncing & Importing to AI Studio" section explaining how to import and how the automated restore works.
