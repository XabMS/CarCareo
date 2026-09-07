# Handoff: CarCareo UI/UX refresh — "Workshop ledger"

## Overview

A visual and structural redesign of CarCareo (Android, Kotlin + Jetpack Compose, offline-first vehicle maintenance tracker). The redesign keeps every existing feature, computation and string meaning, and changes three things:

1. **Look** — a warm paper/utility aesthetic: hairline-ruled tables instead of tonal filled cards, IBM Plex Sans for prose, IBM Plex Mono for anything numeric, status shown as a labelled stripe rather than a 10dp dot.
2. **Navigation** — inside a vehicle, Overview / Plan / History become bottom-nav tabs instead of buttons at the bottom of a scrolling column; a sticky primary action bar (`Log maintenance`) sits above the tabs on every vehicle tab; the garage gets a global "Log work" FAB.
3. **Plan comprehension** — a km-axis timeline plus due-now / soon / later buckets with mini progress bars, added **above** the existing text task list (the list is kept, not replaced).

Source of truth for current behaviour is unchanged: `plan-app-mantenimiento-vehiculos.md` section 2, and task state stays computed, never persisted.

## About the design files

The files in this bundle are **design references authored in HTML** — a prototype of the intended look and layout, not production code to port. The task is to **recreate these screens in the existing Compose codebase** using its established patterns: Material 3 components, `MaterialTheme` roles, `Scaffold`, Navigation Compose, the existing ViewModels and domain layer. Do not introduce a web view, and do not translate the HTML structure literally — read the measurements and tokens below and express them in Compose.

`CarCareo UI.dc.html` renders three groups of phone frames:

- **`1a` — today's build**, recreated from `master` for comparison (brand palette, dynamic colour off). Reference only; do not implement.
- **`1b` — Workshop ledger**, the chosen direction: Garage, Vehicle overview, Log maintenance, Plan setup.
- **`2a` — Workshop ledger, rest of the app**: History, Plan editor, Task editor sheet, full New-vehicle form, Settings & data.
- **`1c` — Calm garage**, a rejected alternative. Ignore.

Implement `1b` + `2a`.

## Fidelity

**High fidelity.** Colours, type sizes, weights, radii, paddings and copy are final and listed below. Recreate them exactly. Where a value is not listed, follow Material 3 defaults and the 4dp/8dp spacing rhythm already used in the app.

Two caveats:

- The frames are drawn at 412 × 892 dp with an 8px bezel; all dp values below are real Android dp.
- The prototype simulates Compose components (switches, checkboxes, chips, bottom sheet). Use the real Material 3 composables and restyle them via theme/colours — do not hand-draw them.

## Theme changes

### Colour

The redesign replaces the generated tonal scheme with a hand-picked one, and **turns Material You dynamic colour off** (`CarCareoTheme(dynamicColor = false)` — or drop the parameter): the paper ground and the traffic-light statuses are the identity, and dynamic colour overrides both. This is a deliberate change from the current default in `ui/theme/Theme.kt`.

Light scheme (this design is light-only; a dark counterpart is not designed yet — keep the existing `DarkColors` until it is):

| Role | Hex | Used for |
| --- | --- | --- |
| `background`, `surface` | `#F6F1E7` | Page ground ("paper") |
| `surfaceContainerLowest` / card ground | `#FFFCF6` | Every framed block, list table, bottom bar |
| `surfaceContainerHigh` / band | `#F1EADC` | Card footer bands, selected task rows, inactive task rows |
| `onSurface` | `#17140F` | Titles, primary values, filled-button ground |
| `onSurface` (body) | `#3B342A` | Body copy inside cards |
| `onSurfaceVariant` | `#6B6153` | All meta/mono labels ≤12sp, supporting copy |
| `outline` | `#DED5C4` | 1dp card borders, section rules |
| `outlineVariant` | `#EDE4D4` | Hairline dividers inside a table |
| decorative outline | `#A79C88`, `#C8BEAC` | Empty checkbox outline, drag handles, chip borders |
| `primary` (accent) | `#8A4A22` | Text-only actions ("SELECT ALL OVERDUE"), attachment/data icons |
| `error` / overdue | `#A32A1E` | Overdue text, stripes, bars |
| soon (text) | `#7A5C0A` | "SOON" labels, upcoming detail text |
| soon (fill) | `#8A6A12` | Upcoming bars, dots, icons (non-text only) |
| ok | `#3D6B3A` | Up-to-date labels, dots |
| alert ground | `#FBEEEA` | Garage "1 task overdue" banner |
| info ground | `#F6F0E0` | Settings backup notice |

Contrast rule that was enforced during design: **any text at ≤12sp clears 4.5:1 against its own ground.** That is why the amber text colour (`#7A5C0A`) differs from the amber fill colour (`#8A6A12`), and why `#6B6153` is the smallest-text meta colour. Keep both distinctions when implementing.

Primary buttons are **ink on paper**, not accent-coloured: container `#17140F`, label `#F6F1E7`.

### Typography

Two families, both OFL-licensed and available on Google Fonts (add as downloadable fonts or bundle the TTFs in `res/font/`):

- **IBM Plex Sans** — prose, titles, list rows. Weights 400 / 500 / 600.
- **IBM Plex Mono** — every number, unit, date, plate, status label and section label. Weights 400 / 500 / 600.

Rewrite `ui/theme/Type.kt` as an explicit `Typography` rather than `Typography()`:

| Token | Family / size / weight | Extras |
| --- | --- | --- |
| Screen title | Plex Sans 20sp / 600 | in a custom app bar, not `TopAppBar`'s `titleLarge` |
| Garage title | Plex Sans 30sp / 600 | letter-spacing −0.02em |
| Section label | Plex Mono 10–11sp / 600 | UPPERCASE, letter-spacing 0.12–0.14em |
| Row title | Plex Sans 15–16sp / 400 | |
| Row meta | Plex Mono 11sp / 400 | UPPERCASE for intervals |
| Big number | Plex Mono 36sp / 600 | letter-spacing −0.02em, unit suffix 16sp/500 in `onSurfaceVariant` |
| Card number | Plex Mono 20–22sp / 600 | |
| Button label | Plex Mono 12–13sp / 600 | UPPERCASE, letter-spacing 0.08em |
| Supporting | Plex Sans 12sp / 400 | `#6B6153` |

Numeric text must use tabular figures where columns align (history costs, km values).

### Shape & elevation

- Cards, tables, buttons, fields: **4dp** radius, 1dp border `#DED5C4`, **no elevation, no shadow** (the border does the work). This replaces the current 12dp filled/elevated cards.
- Chips: 3dp radius, 30dp height.
- Bottom sheet: 12dp top corners.
- The only shadows in the design are on the two floating action bars: `0 6dp 18dp rgba(23,20,15,0.28)`.

### App bar

Replace `TopAppBar` with a compact custom header (it carries two lines):

- Row height 44dp for the icon buttons, container padding `6dp` top / `12dp` horizontal / `10dp` bottom.
- Line 1: screen or vehicle name, Plex Sans 20sp/600, `#17140F`.
- Line 2: Plex Mono 11sp, letter-spacing 0.06em, `#6B6153` — the context string ("HORNET · 14 RECORDS", "PETROL MOTORBIKE · 1234 ABC", "STEP 1 OF 2").
- Navigation icon `arrow_back` / `close` at 22dp, overflow `more_vert` at 22dp, both in 44dp touch targets.

## Navigation changes

`ui/navigation/NavGraph.kt` and `Destinations.kt` change shape:

- `vehicle/{vehicleId}` becomes a **shell with three tabs**: `overview`, `plan`, `history`. The `NavigationBar` and the sticky action bar live in the shell, so all three tabs share them. `Destinations.history(id)` and `Destinations.planEditor(id)` become tab selections inside the shell rather than separate pushes (keep the routes so notifications and deep links still work; they should land on the shell with the right tab selected).
- The Plan tab is the plan **editor** (it already lists tasks with intervals); the Overview tab keeps the read-only status list. There is no longer an "Edit plan" button.
- `log/{vehicleId}` stays a pushed screen, reachable from: the sticky bar in the vehicle shell, the garage FAB ("LOG WORK" — opens a vehicle picker first when more than one vehicle is active), and the garage overdue banner ("LOG", pre-selecting that vehicle and task).
- `vehicleForm` → `planSetup/{id}` is presented as a two-step flow ("STEP 1 OF 2" / "STEP 2 OF 2") with a 2-segment progress bar under the header. Behaviour is unchanged (back from step 2 still lands on the garage).
- Settings and Archived are unchanged structurally.

## Screens

### 1. Garage (`ui/screen/GarageScreen.kt`)

**Purpose:** answer "does anything need work?" without opening a vehicle.

Layout, top to bottom:

- Header: "CarCareo" in Plex Mono 11sp/0.14em `#6B6153`, then "Garage" Plex Sans 30sp/600. Trailing: a 44dp `add` button with a 1dp `#DED5C4` border on `#FFFCF6` (add vehicle), then a borderless 44dp `more_vert`.
- **Attention banner** (new) — only when at least one active vehicle has an overdue task. 20dp side margins, 1dp `#DED5C4` border with a **4dp left border `#A32A1E`**, ground `#FBEEEA`, radius 4dp, padding 12dp/14dp. Contents: `error_outline` 20dp `#A32A1E`; line 1 Plex Mono 11sp/600 `#A32A1E` uppercase — "N TASKS OVERDUE" (plural-aware); line 2 Plex Sans 14sp `#3B342A` — `"<vehicle> · <task>"` for the single most urgent, or "N vehicles" when it spans several; trailing "LOG" Plex Mono 12sp/600 `#A32A1E`, which opens Log maintenance for that vehicle with that task pre-selected.
- **Vehicle cards**, 12dp gap, 20dp side padding. Each card is a 1dp-bordered `#FFFCF6` block, radius 4dp, in three stacked parts:
  1. **Head**, padding 14dp: category icon 26dp `#6B6153` (`two_wheeler` / `directions_car` / `electric_car`, from `CategoryUi.icon`); then name Plex Sans 19sp/600 with the plate beside it in Plex Mono 11sp `#6B6153`; make · model · year on line 2, Plex Sans 13sp `#6B6153`. Right column, right-aligned: odometer in Plex Mono 20sp/600 (grouped, no unit), and under it Plex Mono 10sp/0.1em `#6B6153` — "KM · EST." or "KM · CONFIRMED" from `OdometerReading.isEstimate`.
  2. **Status stripe**, 6dp tall, full card width, replacing the semaphore dot: segments proportional to the plan — overdue share `#A32A1E`, upcoming share `#8A6A12`, remainder `#DED5C4`. With no plan, a single `#DED5C4` bar.
  3. **Status footer**, ground `#F1EADC`, padding 10dp/14dp: state word in Plex Mono 10sp/600/0.1em coloured `#A32A1E` / `#7A5C0A` / `#3D6B3A` ("OVERDUE" / "SOON" / "UP TO DATE"), then the existing one-liner tail (`StatusUi.garageLine` minus its prefix) in Plex Sans 13sp `#3B342A`, ellipsised to one line. With no plan: "NO PLAN" + "No maintenance plan yet".
- **Global FAB**, bottom-right, 20dp inset: 52dp tall, radius 4dp, ground `#17140F`, `build` icon 20dp + "LOG WORK" Plex Mono 13sp/600/0.08em, both `#F6F1E7`.

Empty state keeps the existing strings (`garage_empty_title` / `garage_empty_body`), set in Plex Sans 20sp/600 and 14sp `#6B6153`.

### 2. Vehicle → Overview tab (`ui/screen/VehicleDetailScreen.kt`)

Header context line: "PETROL MOTORBIKE · 1234 ABC".

- **Odometer block** — 1dp bordered `#FFFCF6`, radius 4dp, padding 14dp, row with the number on the left and the action on the right (bottom-aligned):
  - "ODOMETER" Plex Mono 10sp/0.12em `#6B6153`
  - value Plex Mono 36sp/600 `#17140F` + " km" 16sp/500 `#6B6153`
  - freshness line Plex Sans 12sp `#6B6153` — existing `freshnessText()` strings.
  - "UPDATE" — 40dp tall, 1dp `#17140F` border, radius 4dp, padding 0/16dp, Plex Mono 12sp/600/0.06em. Opens the existing Update-odometer dialog unchanged (including the minor/major rollback guards).
- **"Road ahead" timeline** (new) — section label row: "ROAD AHEAD" Plex Mono 11sp/0.12em `#6B6153` on the left, "next 12,000 km" Plex Mono 11sp `#6B6153` on the right. The window is `max(12,000 km, distance to the furthest upcoming task)` rounded up; if every task is overdue, show the overdue cluster at the left edge.
  - Card: 1dp border, `#FFFCF6`, padding 18dp top / 14dp sides / 12dp bottom, inner height 96dp.
  - A 1dp `#DED5C4` horizontal axis at y = 44dp; a 2dp `#17140F` vertical "now" marker at x = 0 with "NOW" (Plex Mono 10sp/600) above and the current km (Plex Mono 10sp `#6B6153`) below.
  - Each active task is a 9dp dot on the axis, positioned by `kmRemaining` (tasks with only a month interval are placed by projected km using the annual estimate; if that is unknown, list them in the buckets only). Dot colour = status colour. Label (Plex Sans 11sp, status colour for overdue/upcoming, `#3B342A` for ok) and its km (Plex Mono 10sp `#6B6153`) alternate above and below the axis to avoid collisions; overdue tasks are pinned to the left edge and read "N over".
  - Cap the number of labelled markers at 4–5; collapse the rest into the buckets below.
- **Buckets** (new, replaces nothing) — three rule-separated groups, in order: "DUE NOW" `#A32A1E`, "SOON" `#7A5C0A`, "LATER" `#3D6B3A`. Header row: label Plex Mono 11sp/600/0.12em + a 1dp `#DED5C4` rule filling the row; "LATER" is collapsed by default and shows its count on the right. Each task row (padding 10dp vertical): name Plex Sans 15sp `#17140F`; meta Plex Mono 11sp `#6B6153` — `"<remaining or over> · every <interval>"` (existing `remainingSummary()` + `intervalSummary()`); a 76 × 6dp mini bar showing interval consumption (`#EDE4D4` track, status-coloured fill, clamped to 100%); `chevron_right` 20dp `#6B6153`. Tapping a row still opens Log maintenance with that task pre-selected. Tasks with no history keep the existing "No previous record" string in place of the meta line.
- Deactivated tasks keep the existing struck-through treatment, listed under the collapsed "LATER" group.
- **Sticky action bar + tabs** — pinned to the bottom, ground `#FFFCF6`, 1dp `#DED5C4` top border, padding 10dp/20dp/8dp:
  - Primary: full-width 48dp, radius 4dp, `#17140F`, `build` 20dp + "LOG MAINTENANCE" Plex Mono 13sp/600/0.08em `#F6F1E7`.
  - Below it a 3-item `NavigationBar`-equivalent: `dashboard` / `checklist` / `history` at 22dp with Plex Mono 10sp/0.06em labels; selected = `#17140F` and 600 weight, unselected = `#6B6153`. Total bar height ≈ 116dp; the scrolling content must reserve that as bottom content padding.

### 3. Vehicle → History tab (`ui/screen/HistoryScreen.kt`)

Header context: "HORNET · N RECORDS".

- **Cost pair** — one 1dp-bordered `#FFFCF6` block split by a 1dp vertical rule: "TOTAL SPENT" and "LAST 12 MONTHS" in Plex Mono 10sp/0.12em `#6B6153`, amounts Plex Mono 22sp/600. Uses the existing `formatCost` (locale currency). Shown only when `state.showCosts`.
- **Task filter chips** — 30dp tall, radius 3dp, Plex Mono 11sp/0.06em uppercase. Selected: ground `#17140F`, label `#F6F1E7`. Unselected: `#FFFCF6` with a 1dp `#DED5C4` border, label `#3B342A`. First chip is "ALL" (`history_filter_all`).
- **Year bands** (new grouping) — a header row per calendar year: year in Plex Mono 11sp/600/0.14em `#6B6153`, a 1dp rule, and that year's total on the right in Plex Mono 11sp `#6B6153`.
- **Record rows** — one 1dp-bordered `#FFFCF6` table per year, rows split by 1dp `#EDE4D4` hairlines, padding 12dp/14dp, three columns:
  - Left, 56dp fixed: date as Plex Mono 13sp/600 "14 MAR" (day + short month, locale-aware) and the record odometer under it in Plex Mono 10sp `#6B6153`.
  - Middle: tasks covered joined with " · " (Plex Sans 15sp), or the existing `history_unplanned` string; workshop and notes under it in Plex Sans 12sp `#6B6153`; when an attachment exists, an `attach_file` 15dp + "INVOICE" Plex Mono 11sp/600, both `#8A4A22`, which fires the existing `ACTION_VIEW` intent.
  - Right: cost Plex Mono 15sp/600 right-aligned, with the row's `more_vert` (delete) under it.
- Deleting still goes through the existing confirm dialog and recalculation. Empty state uses `history_empty`.
- The sticky action bar + tabs are the shell's, unchanged.

### 4. Vehicle → Plan tab (`ui/screen/PlanEditorScreen.kt`)

Header context: "HORNET · N TASKS · M INACTIVE".

- Section label row: "ORDER SHOWN IN THE PLAN" Plex Mono 11sp/0.12em `#6B6153` + rule.
- One 1dp-bordered `#FFFCF6` table, rows split by `#EDE4D4` hairlines, row padding 14dp:
  - `drag_indicator` 20dp `#A79C88` — **replaces the move-up / move-down overflow items** with drag-to-reorder (`Modifier.draggable` or a reorderable list; persist through the existing `viewModel.move`, keeping `sortOrder` semantics).
  - Name Plex Sans 16sp `#17140F`; interval Plex Mono 11sp `#6B6153` uppercase (`intervalSummary()`, e.g. "EVERY 6,000 KM · 12 MONTHS"); when a warn threshold is set, a third line Plex Mono 10sp `#6B6153` — "WARN 500 KM / 30 DAYS BEFORE".
  - Trailing `more_vert` 20dp `#6B6153` → Edit, Activate/Deactivate, Delete (no move items).
  - **Inactive rows**: ground `#F1EADC`, name struck through `#6B6153`, drag handle `#C8BEAC`, plus a 24dp chip (1dp `#C8BEAC`, radius 3dp) reading "INACTIVE · TAP TO RESUME" in Plex Mono 10sp/600/0.08em `#6B6153`, which toggles active.
- **Template top-up row** (new) — shown when the vehicle's category template contains tasks the plan lacks: 1dp **dashed** `#C8BEAC` border, radius 4dp, padding 12dp/14dp, `library_add` 20dp `#8A4A22`, "Add the rest of the petrol motorbike template" Plex Sans 14sp `#3B342A`, count on the right in Plex Mono 11sp `#6B6153`. Tapping it appends only the missing tasks and shows the existing `template_disclaimer` in a confirm dialog.
- **Add-task FAB**: bottom-right, 20dp inset, 52dp tall, radius 4dp, `#17140F`, `add` 20dp + "ADD TASK" Plex Mono 13sp/600/0.08em.
- Empty state keeps `plan_empty_title` / `plan_empty_body` / `plan_apply_template`.

### 5. Task editor sheet (`ModalBottomSheet` in `PlanEditorScreen.kt`)

Ground `#FFFCF6`, 12dp top corners, 36 × 4dp `#DED5C4` drag handle, padding 8dp top / 20dp sides / 20dp bottom, 12dp gaps.

- Title row: "New task" / "Edit task" Plex Sans 20sp/600 with the vehicle name on the right in Plex Mono 11sp/0.06em `#6B6153`.
- Fields are **labelled above**, not with floating labels: label Plex Mono 10sp/0.12em `#6B6153`, then a 48dp box (1dp border, radius 4dp, 14dp horizontal padding). Focused/primary field border `#17140F`, others `#DED5C4`. Error state: border and label `#A32A1E` with the existing error strings.
  - "TASK NAME" — text.
  - "INTERVAL · WHICHEVER COMES FIRST" — two side-by-side boxes (10dp gap) with the value in Plex Mono 16sp/600 left and the unit ("KM", "MONTHS") in Plex Mono 11sp `#6B6153` right. Helper below, Plex Sans 12sp `#6B6153`: "Fill in at least one. Leave a field empty to ignore that dimension." (replaces `field_interval_help` — new string, same rule).
  - "WARN ME BEFORE" — same pattern with "KM" and "DAYS".
  - "NOTES" — multi-line, min 48dp.
  - Active row: `#F1EADC` ground, 1dp border, "Active" Plex Sans 15sp with "Inactive tasks keep their history but stop being tracked." 12sp `#6B6153` under it, and a Material `Switch` on the right (track `#17140F` when on, `#DED5C4` when off; thumb `#F6F1E7`).
- Buttons, 10dp gap, 48dp tall: "CANCEL" (1 part, 1dp `#DED5C4` border, Plex Mono 12sp/600/0.08em `#3B342A`) and "SAVE TASK" (2 parts, `#17140F` ground, `#F6F1E7` label).
- Scrim behind the sheet: `rgba(23,20,15,0.32)`.

### 6. New vehicle (`ui/screen/VehicleFormScreen.kt`)

Header context: "STEP 1 OF 2", with a 2-segment progress bar under it (3dp tall, 4dp gap, filled `#17140F`, empty `#DED5C4`). In edit mode the step line and progress bar are omitted and the title is "Edit vehicle".

- **Category picker first** — three equal tiles in a row (8dp gap), each 1dp bordered, radius 4dp, `#FFFCF6`, padding 10dp/6dp: category icon 24dp above a Plex Mono 9sp/0.06em uppercase label ("MOTORBIKE", "PETROL CAR", "ELECTRIC CAR"). Selected: border `#17140F`, icon and label `#17140F` at 600. Unselected: border `#DED5C4`, `#6B6153`. This replaces the `ExposedDropdownMenuBox`.
- **Name** — labelled-above field, 48dp, focused border `#17140F`.
- **"THE VEHICLE" table** — section label + rule, then one bordered table with hairline-separated rows (padding 12dp/14dp), each row a 78dp Plex Mono 11sp `#6B6153` label ("MAKE", "MODEL", "YEAR", "PLATE", "BOUGHT") and the value in Plex Sans 15sp (Plex Mono for year and plate). Empty optional values read "Optional" in `#6B6153`; the date row carries an `event` 18dp trailing icon and opens the existing `DatePickerField` dialog.
- **"MILEAGE" table** — same pattern: "CURRENT" (value Plex Mono 17sp/600 + "KM" suffix), "READ ON" (date + `event`), "PER YEAR" (Plex Mono 15sp + "KM"). Helper under the table, Plex Sans 12sp `#6B6153`: "The yearly figure is only used to estimate km between confirmations."
- Notes field (labelled-above, multi-line) sits after the mileage table.
- **Sticky footer**: `#FFFCF6`, 1dp top border, padding 12dp/20dp; full-width 48dp `#17140F` button, "SAVE & SET UP PLAN" Plex Mono 13sp/600/0.08em + `arrow_forward` 20dp. In edit mode the label is "SAVE" with no icon.
- Validation is unchanged (`error_name_required`, `error_km_invalid`, scroll-to-top + snackbar on a rejected save); errors paint the offending field's border and label `#A32A1E`.

### 7. Plan setup (`ui/screen/PlanSetupScreen.kt`)

Header "Maintenance plan", context "STEP 2 OF 2 · HORNET", both progress segments filled. Back is allowed and still lands on the garage.

- Intro, Plex Sans 14sp `#4A443C`: "Pick a starting point. Everything can be changed later, task by task."
- **Recommended card** — 1dp `#17140F` border, `#FFFCF6`, radius 4dp, padding 14dp, with a "RECOMMENDED" tag overlapping the top-left corner (ground `#17140F`, label `#F6F1E7`, Plex Mono 9sp/600/0.1em, radius 2dp, 2dp/6dp padding).
  - Title "<Category> template" Plex Sans 17sp/600; subtitle "N tasks with guideline intervals" 13sp `#6B6153`.
  - **Preview of the template** (new): the first three tasks as Plex Mono 11sp rows — name left `#3B342A`, interval right `#6B6153` — then "+ N more" `#6B6153`. Read from the existing `domain/` templates.
  - 44dp `#17140F` button "USE THIS TEMPLATE"; the existing `template_disclaimer` under it in Plex Sans 11sp `#6B6153`.
- Two secondary rows, 1dp `#DED5C4`, `#FFFCF6`, padding 14dp: `content_copy` → "Copy another vehicle's plan" with the candidate names underneath (`Ibiza · Zoe`), and `edit_note` → "Start from scratch" / "Add tasks yourself". Each with a trailing `chevron_right`. The copy row is hidden when there are no other vehicles.

### 8. Settings & data (`ui/screen/SettingsScreen.kt`)

Three labelled groups ("YOUR DATA", "REMINDERS", "ABOUT"), each a section label (Plex Mono 10sp/600/0.14em `#6B6153`) + rule, then a 1dp-bordered `#FFFCF6` table with hairline-separated 14dp rows. Section headers are no longer `primary`-coloured.

- **Your data**: `upload_file` "Export a backup" — "Every vehicle, plan and record in one JSON file. Invoice attachments are not included." · `download` "Import a backup" — "Add to what you have, or replace everything." · `inventory_2` "Archived vehicles" with the count in Plex Mono 12sp and a `chevron_right`. Icons 22dp, `#8A4A22` for the two data actions, `#6B6153` for archived.
- **Backup notice** (new) — under the table: 1dp `#DED5C4` border with a 4dp left border `#8A6A12`, ground `#F6F0E0`, `info` 18dp, Plex Sans 12sp `#3B342A`: "Everything lives on this phone only. A backup is the only copy — last one was N months ago." Persist the last export timestamp in the existing prefs to fill in the tail; omit the tail when there has never been an export.
- **Reminders**: the two existing toggles with their existing description strings, as rows with a Material `Switch` (track `#17140F` on / `#DED5C4` off, thumb `#F6F1E7`). Permission handling and WorkManager scheduling unchanged.
- **About**: a single row, "CarCareo" Plex Sans 15sp `#3B342A` left, "VERSION <name>" Plex Mono 12sp `#6B6153` right.
- Import mode dialog, replace confirmation, snackbars and busy `LinearProgressIndicator` are unchanged in behaviour; the progress indicator should use `#17140F`.

Archived vehicles keeps its current structure, restyled as one bordered table with hairline rows and a Plex Mono "UNARCHIVE" text action.

## Interactions & behaviour

Nothing in the domain layer changes. New or changed interactions only:

- Garage banner "LOG" → `log/{vehicleId}?taskId={taskId}` for the most urgent overdue task.
- Garage FAB → vehicle picker (bottom sheet listing active vehicles, same row style as the garage cards' head) when more than one active vehicle, else straight to `log/{vehicleId}`.
- Vehicle shell tab switches keep scroll position per tab and do not re-enter the back stack; system Back from any tab leaves the vehicle.
- Sticky bar is always visible; content scrolls under it with bottom content padding equal to the bar height.
- Timeline markers are tappable with the same target as the bucket rows (log that task); minimum 44dp touch height on all rows, chips and toggles.
- Plan tab drag-to-reorder: long-press to lift, haptic on lift, commit on drop through `viewModel.move`.
- "LATER" bucket expand/collapse is animated (`animateContentSize`, default duration).
- Template top-up appends only missing template tasks, after the disclaimer dialog.
- Everything else — odometer rollback guards, pre-checked overdue/upcoming tasks on the log screen, "select all overdue", free-form records, attachment picker with persistable URI grant, delete-record recalculation, export/import modes — behaves exactly as it does today.

## State management

No new ViewModels. Additions:

- Vehicle shell: `selectedTab` (Overview / Plan / History), hoisted in the shell composable and saved with `rememberSaveable`.
- Overview: `laterExpanded: Boolean` (`rememberSaveable`).
- Garage: derived `overdueSummary` (count + most urgent vehicle/task) computed from the existing `VehicleCard` list; no new persistence.
- Garage FAB: `showVehiclePicker: Boolean`.
- Settings: `lastExportAt: Instant?` added to the existing prefs store, written after a successful export.
- Timeline: pure derivation from `VehiclePlanStatus` — window size, marker positions, label side. Put it in `domain/` (or `ui/plan/`) as a pure function and unit-test it alongside the existing engine tests.

## Design tokens

**Colour** — see the table under "Theme changes".

**Spacing** — 4 / 6 / 8 / 10 / 12 / 14 / 18 / 20 dp. Screen side padding 20dp. Card padding 14dp. Table row padding 12–14dp. Card gap 12dp.

**Radius** — 2 (tags), 3 (chips), 4 (cards, tables, fields, buttons, FABs), 12 (bottom sheet top), 50% (status dots).

**Borders** — 1dp `#DED5C4` (card/table), 1dp `#EDE4D4` (inner hairline), 4dp left accent (banners), 1dp dashed `#C8BEAC` (template top-up), 2dp `#17140F` (timeline "now" marker).

**Sizes** — icon buttons 44dp, icons 18 / 20 / 22 / 24 / 26dp, primary bar button 48dp, floating bars 52dp, chips 30dp, fields 48dp, status stripe 6dp, mini bars 76 × 6dp, timeline dots 9dp, inner timeline height 96dp.

**Shadow** — `0 6dp 18dp rgba(23,20,15,0.28)` on floating bars only.

## Assets

- **Icons**: Material Icons (filled) only — `two_wheeler`, `directions_car`, `electric_car`, `add`, `more_vert`, `arrow_back`, `close`, `chevron_right`, `keyboard_arrow_right`, `event`, `build`, `check`, `dashboard`, `checklist`, `history`, `drag_indicator`, `library_add`, `content_copy`, `edit_note`, `error_outline`, `info`, `attach_file`, `upload_file`, `download`, `inventory_2`, `arrow_forward`, `expand_more`, `receipt_long`. All exist in `androidx.compose.material:material-icons-extended`, already a dependency. The HTML prototype loads them from the Material Icons webfont — no icon was hand-drawn, and none should be.
- **Fonts**: IBM Plex Sans and IBM Plex Mono (SIL OFL 1.1). Bundle the needed weights in `res/font/` or use downloadable fonts.
- No images, illustrations or raster assets.
- Sample content in the frames (Hornet / Ibiza / Zoe, workshop names, costs) is placeholder data.

## Files

- `CarCareo UI.dc.html` — the design. Sections, top to bottom: `2a` (History, Plan editor, Task sheet, New vehicle, Settings), `1b` (Garage, Vehicle overview, Log maintenance, Plan setup), `1c` (rejected alternative — ignore), `1a` (recreation of the current build, for comparison).
- `android-frame.jsx`, `support.js` — the prototype's device frame and runtime. Reference only; nothing to port.
- `screenshots/1b-garage-overview-log-plansetup.png` — the four core screens of the chosen direction.
- `screenshots/2a-history-plan-taskssheet-form-settings.png` — History, Plan editor, Task sheet, New vehicle, Settings.
- `screenshots/1a-current-build-recreation.png` — the current build, for before/after comparison.
- `github.md` — the upstream association (`XabMS/CarCareo`, branch `master`) and a screen → source-file map.

Open `CarCareo UI.dc.html` in a browser to see the frames; it needs no build step.
