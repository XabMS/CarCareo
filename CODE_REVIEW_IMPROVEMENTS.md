# CarCareo — Code Review & Improvement Plan

> Review date: 2026-09-14
> Updated: 2026-09-14 — every item from the original review except the four
> remaining polish items below has now been fixed and verified: odometer
> severity/major-decrease gate, future-date rejection, invalid-cost surfacing
> in the log flow; the `VehicleShellScreen` history ViewModel no longer runs
> on every tab; backup import reports unresolved record→task links; the
> unique `(vehicleId, name)` DB constraint (with migration + migration test);
> `MaintenanceRecordRepository`/`MaintenanceTaskRepository` are now
> transactional, with a UI-level double-tap guard on both Save buttons;
> ViewModel, repository, and domain edge-case test coverage was added; and a
> GitHub Actions CI workflow now runs the unit tests + lint on every push/PR.
> Worker dependency-injection cleanup was dropped from scope entirely (see the
> note at the bottom) rather than fixed.
> Scope: full Android project (`app/src/main/java/com/xabier/carcareo`)
> Build checks run: `./gradlew :app:testDebugUnitTest` ✅, `./gradlew :app:lintDebug` ✅,
> and `./gradlew :app:connectedDebugAndroidTest` ✅ (migration + backup round-trip)

---

## 1. Executive Summary

CarCareo is a well-architected, offline-first Android app with a clear separation between `data`, `domain`, and `ui` layers. The domain logic is pure Kotlin (no Android/Room dependencies) and is thoroughly unit-tested. The build is clean, lint passes, and the privacy-first design (no network permission, manual JSON backup, `allowBackup=false`) is excellent.

Every correctness, data-integrity, and test-coverage item from the original review has been addressed. What's left is purely polish (notification id, screen size, accessibility, deep links) — none of it urgent.

---

## 2. Remaining Polish

### 2.1 Notification ID can overflow

**Problem**
`Notifications.kt:77` generates the per-vehicle notification id with:

```kotlin
nm.notify(VEHICLE_ID_OFFSET + vehicleId.toInt(), perVehicle)
```

`vehicleId` is a `Long`. After many add/delete cycles the id can exceed `Int.MAX_VALUE - VEHICLE_ID_OFFSET`, causing collisions or negative notification ids.

**Suggested fix**
Use a stable hash-based id, e.g.:

```kotlin
val notificationId = VEHICLE_ID_OFFSET + vehicleId.hashCode()
nm.notify(notificationId, perVehicle)
```

or maintain a small integer mapping in `AppPreferences`.

**Files to change**
- `app/src/main/java/com/xabier/carcareo/notifications/Notifications.kt`

*Technically correct, but on a personal single-user app this would take billions of vehicle inserts (Room's autoincrement id) to actually collide — negligible real-world risk. Fine to fold in with other polish work rather than treat as urgent.*

### 2.2 Split oversized screens

Several screens are 300–500 lines long. Consider extracting private composables for readability and `@Preview` support:
- `GarageScreen.kt`
- `VehicleFormScreen.kt`
- `PlanEditorScreen.kt`
- `SettingsScreen.kt`
- `VehicleDetailScreen.kt`

### 2.3 Accessibility

Some icon buttons use `contentDescription = null` (e.g. in `HistoryScreen.kt`). Add meaningful descriptions for screen readers.

### 2.4 Notification deep link

Tapping a maintenance notification currently opens the app at the garage. Consider passing the `vehicleId` in the `PendingIntent` and navigating directly to that vehicle.

---

## 3. Notes

- All file paths are relative to `app/src/main/java/com/xabier/carcareo/` unless stated otherwise.
- Line numbers refer to the codebase as of the original review date and may have shifted after the fixes described above.
- The project already enforces `HardcodedText` as a lint error, so any new UI strings must be added to `res/values/strings.xml` and `res/values-es/strings.xml`.
- **Dropped from scope**: worker dependency-injection cleanup (`MaintenanceCheckWorker`/`OdometerReminderWorker` casting `applicationContext` to `CarCareoApp` instead of going through a `WorkerFactory`). Real inconsistency, no active bug, and one of the review's own suggested fixes (passing repositories through `WorkerParameters`) doesn't actually work as written (input data must be primitives/strings) — not worth the `WorkerFactory` boilerplate for two workers right now.
- **Test infrastructure added**: Robolectric (`app/src/test`) is now used alongside the existing pure-domain tests to exercise ViewModels and repositories against a real in-memory Room DB — see `LogMaintenanceViewModelTest`, `PlanEditorViewModelTest`, `VehicleFormViewModelTest`, `MaintenanceRecordRepositoryTest`, `MaintenanceTaskRepositoryTest`. The Room migration test (`MigrationTest`) lives under `app/src/androidTest` instead, alongside the pre-existing `BackupRoundTripTest`, since `MigrationTestHelper` needs a real `Instrumentation` — CI only runs the `app/src/test` suite, so this one test still needs a manual `connectedDebugAndroidTest` run to re-verify after any future schema change.
