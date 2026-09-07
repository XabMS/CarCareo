# CarCareo

Android app to track the maintenance of your personal vehicles. Offline‑first,
no accounts, no backend, no network. Your data stays on the device and travels
via JSON export/import.

## Features

- **Garage** — manage several vehicles (petrol motorbike, petrol car, electric
  car). Archive the ones you no longer own without losing their history.
- **Odometer** — quick "update km" with an estimated‑km projection between
  updates and a guard against accidental large rollbacks.
- **Maintenance plan** — per‑vehicle task list with km / month intervals.
  Start from a built‑in template, duplicate another vehicle's plan, or build
  it from scratch.
- **Computed status** — task state (overdue / upcoming / ok) is never stored,
  it is always recalculated from records and odometer. Garage shows a
  per‑vehicle semaphore and one‑line summary.
- **Logging** — one screen to record an intervention, pre‑filled with the
  current km and the overdue/upcoming tasks pre‑checked. Free‑form records
  (not tied to a plan task) are allowed.
- **History & costs** — timeline per vehicle, filter by task, total and
  last‑12‑months cost, file attachments per record.
- **Export / Import** — full backup to a single JSON file (replace or merge).
- **Notifications** — optional daily odometer reminder and maintenance check
  (off by default).
- **Bilingual** ES / EN, driven by the device language. Units: km and months.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room (local database), kotlinx.serialization (backups)
- WorkManager (notifications)
- Manual DI (`di/AppContainer` + `ui/AppViewModelProvider`)
- Pure, unit‑tested domain layer (`domain/`) — the maintenance calculation
  engine has no Android dependencies

`minSdk 26 · targetSdk 37 · applicationId com.xabier.carcareo`

## Project layout

```
app/src/main/java/com/xabier/carcareo/
  data/        Room entities, DAOs, repositories, backup codec, prefs
  domain/      pure calculation engine, estimators, templates, rules
  di/          manual dependency container
  notifications/  WorkManager workers, alert dedup
  ui/          Compose screens, view models, theme, navigation
```

## Build

No Gradle on PATH — use the wrapper. Point `JAVA_HOME` at Android Studio's JBR:

```bash
export JAVA_HOME=/path/to/android-studio/jbr
export ANDROID_HOME=$HOME/Android/Sdk

./gradlew :app:assembleDebug --console=plain
```

Checks:

```bash
./gradlew :app:testDebugUnitTest   # domain unit tests
./gradlew :app:lintDebug           # lint (HardcodedText promoted to error)
```

The instrumented backup round‑trip test (`BackupRoundTripTest`) needs a device
or emulator and is run from Android Studio.

## Design notes

- **Decisions in `plan-app-mantenimiento-vehiculos.md` (section 2) are fixed.**
  That spec is the source of truth for behaviour.
- Task state is computed, never persisted.
- The app is local‑only: `allowBackup=false`, no cloud sync by design. The
  database currently ships without a Room migration path, so the first schema
  change will require one.
