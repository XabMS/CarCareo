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

### 2.1 Split oversized screens

Several screens are 300–600 lines long. Consider extracting private composables for readability and `@Preview` support:
- `GarageScreen.kt`
- `VehicleFormScreen.kt`
- `PlanEditorScreen.kt`
- `SettingsScreen.kt`
- `VehicleDetailScreen.kt`

### 2.2 Accessibility

Some icon buttons use `contentDescription = null` (e.g. in `HistoryScreen.kt`). Add meaningful descriptions for screen readers.

---

## 2b. Second review (2026-11-17) — all fixed

**Bugs**
- Vehicle form had no double-tap guard: two quick taps on Save inserted the vehicle twice.
- Tasks with no history could never become overdue on a vehicle without a purchase date: their baseline was the *last* confirmed odometer, which moves with every update. The vehicle now stores its first reading (`Vehicle.initialKm` / `initialKmDate`, schema v3, `MIGRATION_2_3`) and that is the baseline.
- The "large km decrease" confirmation fired when logging or editing *old* work. It now only applies to a record that is not older than the confirmed reading.
- Maintenance alerts were only checked weekly, which made `AlertDedup` a no-op and could delay an alert by a week. The check is now daily; the dedup limits repeats to weekly. Its memory is cleared when a vehicle is back to OK, when work is logged, and when the vehicle is archived or deleted.
- Switching the odometer reminder on fired it immediately (missing initial delay).
- "Update odometer" accepted a date older than the stored reading.
- A double tap on Back could pop the garage too and leave a blank screen; double taps on a card opened it twice. Navigation is now guarded by the source entry's lifecycle.
- Moving the record that confirmed the odometer to an older date left the vehicle on that old date; it now falls back to the newest record.

**Validation**
- Vehicle form: future reading / purchase date, purchase after reading, and an unconfirmed large km decrease when editing.
- Plan editor: an interval too large to parse is an error instead of "no interval".
- Backup import: future dates, negative warning margins and negative costs are rejected.
- A failed record save no longer crashes; the form stays and shows a message.

**Cleanup**
- CI no longer runs twice per PR commit; the new migration test runs on the JVM, so it is in CI.
- Export reads everything in one transaction with three queries instead of two per vehicle.
- `setArchived` / `confirmOdometer` are targeted `UPDATE`s.
- History's 12-month cost uses `todayFlow()`.
- SAF read grants are released once no record references the attachment.
- Notification id can no longer overflow.

**Features**
- Delete a vehicle permanently (from Archived vehicles).
- The km field of a new record follows the picked date until typed, and is labelled as an estimate.
- Date pickers no longer offer future days (one shared dialog).
- Tapping a maintenance notification opens that vehicle; the notification is cancelled when stale.
- A denied notification permission switches the reminder toggles back off.
- A half-filled record survives process death (`SavedStateHandle`).
- Currency setting for costs.
- An ADD import warns when vehicle names already exist.

---

## 3. Notes

- All file paths are relative to `app/src/main/java/com/xabier/carcareo/` unless stated otherwise.
- Line numbers refer to the codebase as of the original review date and may have shifted after the fixes described above.
- The project already enforces `HardcodedText` as a lint error, so any new UI strings must be added to `res/values/strings.xml` and `res/values-es/strings.xml`.
- **Dropped from scope**: worker dependency-injection cleanup (`MaintenanceCheckWorker`/`OdometerReminderWorker` casting `applicationContext` to `CarCareoApp` instead of going through a `WorkerFactory`). Real inconsistency, no active bug, and one of the review's own suggested fixes (passing repositories through `WorkerParameters`) doesn't actually work as written (input data must be primitives/strings) — not worth the `WorkerFactory` boilerplate for two workers right now.
- **Not verified on a device**: the second review's changes were compiled, linted and unit-tested (Robolectric), but the instrumented tests and the UI itself were not run on an emulator or phone.
- **Test infrastructure added**: Robolectric (`app/src/test`) is now used alongside the existing pure-domain tests to exercise ViewModels and repositories against a real in-memory Room DB — see `LogMaintenanceViewModelTest`, `PlanEditorViewModelTest`, `VehicleFormViewModelTest`, `MaintenanceRecordRepositoryTest`, `MaintenanceTaskRepositoryTest`. The Room migration test (`MigrationTest`) lives under `app/src/androidTest` instead, alongside the pre-existing `BackupRoundTripTest`, since `MigrationTestHelper` needs a real `Instrumentation` — CI only runs the `app/src/test` suite, so this one test still needs a manual `connectedDebugAndroidTest` run to re-verify after any future schema change.
