# Smart Life UI patches

Target: `com.tuya.smartlife` **7.12.0**, version code **872**, minimum Android API 24.
The supplied APKM in `res-apk/` contains `base.apk` and configuration splits; it
does not contain an already decoded source tree. Analysis used the decoded base
APK in the ignored `work/smartlife/` directory.

Seven patches are selectable in Morphe. Every selected patch automatically adds
its toggle to a **Morphe** group inside the existing Settings list. A single patch
produces one toggle; two selected patches produce two toggles. The shared settings
dependency has no name and is not exposed in Morphe's patch list. All included
toggles default to **on**. Stored preferences cannot activate an omitted patch.

| Patch / toggle | Verified target and implementation |
| --- | --- |
| Hide Home Plus | `AIPlusMenuItem.kt` (`ga`), whose view calls `AIAssistantEnableService.L2()` to create `AIPlusMenuView` using `ai_plus_menu_view` / `ai_ic_plus`. Overrides the native menu item's `isEnabled()` check, preventing inflation and reserved toolbar space. The separate add-device `+` menu item is untouched. |
| Hide Home Recommendations | `HomeRecoSlotsCard` / `HomeRecoSlotsViewController`, `HomeCardData.DATA_TYPE_FIND_MORE = 104`. Filters the complete recommendation group from `AsyncSimpleList` submissions before layout/diff calculation, including its title and margins. Only matching `HomeCardData` entries are removed; the backing Home model and all other presentation-list types are preserved. |
| Hide AI Input Bar | Shared `AIBarView` and its Home/floating variants, plus `AIHomeEntryView` and `InnerFloatAIBarLayout` containers. Constructor hooks and component-local visibility overrides collapse the complete component with `GONE`, including wrapper padding. Weak references track the last visibility requested by the app, allowing immediate restoration when disabled. Search fields and the chat editor are not targeted. |
| Hide Intelligence Recommendations | `scene_discover` maps to page ID **3000** in `SceneViewPagerFragment`. Filters both `DefaultSceneTabImpl.b(Map)` and the scene pager's composed page IDs, preserving the remaining order. Saved pager state is wrapped with the toggle value; a mismatched position-based snapshot is rebuilt safely. This does not filter Home cards. |
| Hide Explorer Tab | `TabConfig.name = thing_smart_tab` with `SmartTabGetter` (whose indicator uses localized `home_nav_agent`). Filters the app-shell configuration before indicator and fragment lists are built/reindexed. Maps a request for the removed destination to `thing_device`. App-shell fragment IDs are namespaced by navigation mode, preventing a previously saved Explorer fragment from occupying another tab's slot. Switching this option rebuilds main-tab fragment state; both modes also avoid pre-patch positional tags. Distinct tabs using the same Fragment class retain distinct IDs. |
| Hide Tuya AI Web Version | `WebAppCell.e(Continuation)` returns a boxed false for the native Archer visibility predicate while enabled. Disabled takes the complete original path, including cloud/configuration eligibility. |
| Hide Centre d’avantages | `WelfareEntryCell.e(Continuation)`, identified by `me_entry_list_row` and localized `welfare_center_entry_title`. The same native visibility predicate is independently gated. The Web Version cell and other Profile content are untouched. |

## Native Settings integration

`SettingActivity.initData()` chooses a remote configuration or
`personal_setting_config_list.json` and passes groups, custom cells, and
cell-show overrides to `ArcherContainer`. The settings dependency hooks the
converged constructor call, derives its argument registers, and copies/appends
one group plus custom cells. Existing inputs are not mutated, and repeated
population replaces the Morphe group/cells instead of duplicating them.

`MorpheSwitchCell` inherits Smart Life's `SwitchTextCell`, the same base used by
`SoundCell`. This reuses `baselist_delegate_text_switch2`, native `SwitchButton`,
theme colors, typography, spacing, and the Archer group's backgrounds/dividers.
The Morphe title inflates the native `baselist_delegate_text_title`. There is no
new Activity, manifest entry, external UI library, or replacement design system.
Labels are supplied in English and French.

The runtime uses a separate `morphe_smartlife` Android `SharedPreferences` file.
Each toggle writes only its own key, and a recycled row clears its old switch
listener before binding. Availability methods are initially false in the runtime
DEX; each selected bytecode patch changes only its own method to true. The runtime
DEX is merged exactly once through the unnamed dependency.

AI visibility changes refresh existing tracked components immediately. Other
changes recreate only `FamilyHomeActivity`, once it resumes after Settings closes.
Recreation is posted after the resume callback and skipped for a finishing
Activity. Device-control screens and Settings itself are not recreated. Reopening
other cached screens permits their normal native construction/visibility logic
to run with the new values. Eligibility-controlled items are restored according
to the original app rules; disabling a toggle does not force unavailable content
to appear.

## Files and build

- `patches/src/main/kotlin/app/morphe/patches/smartlife/UiPatches.kt`: seven named patches.
- `patches/src/main/kotlin/app/morphe/patches/smartlife/shared/`: compatibility,
  unnamed settings dependency, and checked bytecode helpers.
- `runtime/smartlife/src/main/java/`: preferences, native cells, settings
  population, and presentation filters.
- `runtime/smartlife/src/stubs/`: handwritten, compile-only native API signatures.
- `runtime/smartlife/src/test/` and `tests/smartlife/VerifySmartLife.java`: runtime
  combination tests and real-APK DEX verification.
- `settings.gradle.kts` includes the Java runtime module;
  `patches/build.gradle.kts` embeds its D8 output as `extensions/smartlife.mpe`.

Java 21 is required. The runtime is compiled for Java 8 / Android API 24 and uses
only APIs available to this target. Android/XMLPull compile APIs and D8 are build
dependencies; no external runtime dependency or app stub is bundled.

```sh
./gradlew buildAndroid :smartlife-runtime:verifyRuntime
```

The normal build requires the existing GitHub Packages credentials documented in
`AGENTS.md`. No credentials or extracted APK files are part of this change.
Generated release metadata is deliberately left to the existing release process.

## Validation results

- All Kotlin patch sources and the Java runtime compiled using the existing
  isolated `work/validation` Gradle project and the local Morphe Desktop 1.18.1
  distribution. The standard root build could not resolve the private
  `app.morphe.patches:1.3.4` Gradle plugin from its configured repositories/cache.
- JVM tests passed **128 patch selections × 128 toggle states**, checking default
  values, exact settings population, idempotency, independent filtering,
  disable/restore paths, reconstruction of the preference reader, immutable input
  preservation, navigation order, and selection fallback. These use an in-memory
  SharedPreferences substitute; they are not an Android process-restart test.
- Morphe successfully applied and rebuilt APKs for each of the seven patches
  alone, Home Plus + Home Recommendations, the two Profile patches together, and
  all seven together.
- The APK verifier checks selected availability constants, native/extension method
  resolution, one shared settings integration, absence of unrelated hooks in
  single-patch builds, valid register/branch boundaries, and preservation of the
  original classes and preexisting duplicate-class counts. The base APK already
  contains some duplicate library classes; the patches do not introduce more.
- The CLI patch listing exposes exactly seven Smart Life patches, all enabled by
  default, and no settings-panel patch.
- D8 compiled the patch bundle for Android as well as the runtime extension. The
  local validation artifact is `work/smartlife/smartlife-patches.mpp`, version
  `1.3.0-dev.1-smartlife-validation`. It includes the existing bundle's other
  patches, JVM patch classes, Android patch DEX, and one Smart Life extension.
  This is a validation build, not a published release.

Local reports are in `work/smartlife/final-build.log`, `final-cases.log`,
`final-dex-verification.log`, and `project-build.log`. The real-APK verifier can be
run independently with the Morphe Desktop fat JAR on the classpath:

```sh
javac -cp path/to/morphe-desktop-all.jar -d work/smartlife/verification tests/smartlife/VerifySmartLife.java
java -cp path/to/morphe-desktop-all.jar:work/smartlife/verification VerifySmartLife original.apk patched.apk 127
```

The last argument is the selection bitmask: Home Plus = 1, Home Recommendations
= 2, AI = 4, Intelligence Recommendations = 8, Explorer = 16, Web Version = 32,
Benefits = 64. All seven together = 127.

## Remaining device validation

There is no attached Android device or emulator in this workspace. The patched
APK has not been launched here. Visual alignment, theme changes, accessibility,
configuration-split installation, real process-death persistence, and absence of
runtime crashes therefore remain **unverified**. APK rebuilding and structural
DEX checks do not establish those properties.

On a device with the matching APK splits, check each toggle alone and together,
leave and reopen each tab, kill/relaunch the process, and switch light/dark mode.
Select Explorer before enabling its hide option, then rotate/relaunch and verify
all remaining tabs and their content. For AI, verify both Home and floating
instances, including components already present when the option is disabled.
Verify a one-patch and a two-patch installation display exactly one and two
Morphe switches respectively. The UI patches do not change authentication,
device control, network endpoints, subscription eligibility, or content access.
