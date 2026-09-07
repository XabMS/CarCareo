repo: XabMS/CarCareo
branch: master

## Last sync

date: 2026-09-07T18:13:00Z

### Updated in this project

- Recreated the current Garage, Vehicle detail, Log maintenance, New vehicle and Plan setup screens from `master`.
- Chosen direction "Workshop ledger" (warm paper, tabular, mono numerals) applied across the app.
- Added redesigns for History, Plan editor + task sheet, full vehicle form and Settings.
- Structural changes: vehicle-level bottom nav, sticky primary action bar, global log shortcut, km-axis plan timeline + due/soon/later buckets.

## Screen map

| Project screen | Repo files |
| --- | --- |
| Garage | `ui/screen/GarageScreen.kt`, `ui/vehicle/CategoryUi.kt`, `ui/vehicle/OdometerUi.kt`, `ui/plan/StatusUi.kt` |
| Vehicle detail | `ui/screen/VehicleDetailScreen.kt`, `ui/plan/StatusUi.kt` |
| Log maintenance | `ui/screen/LogMaintenanceScreen.kt`, `ui/log/LogMaintenanceViewModel.kt` |
| History | `ui/screen/HistoryScreen.kt`, `ui/history/HistoryViewModel.kt`, `ui/format/Format.kt` |
| Plan editor + task sheet | `ui/screen/PlanEditorScreen.kt`, `ui/plan/TaskUi.kt`, `ui/plan/TaskDraft.kt` |
| New vehicle | `ui/screen/VehicleFormScreen.kt`, `ui/component/DatePickerField.kt` |
| Plan setup | `ui/screen/PlanSetupScreen.kt` |
| Settings & data | `ui/screen/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `ui/screen/ArchivedVehiclesScreen.kt` |
| Palette / type / nav (all screens) | `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`, `ui/navigation/NavGraph.kt`, `res/values/strings.xml` |
