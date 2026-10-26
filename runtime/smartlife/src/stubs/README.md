These are handwritten, signature-only compile stubs for the Smart Life 7.12.0 APIs
used by the extension. They are not extracted application implementations.

The `stubs` source set is compile-only. Neither it nor the test source set is
included in `smartlife-runtime.jar` or `extensions/smartlife.mpe`. The real app
supplies the classes at runtime. The small getter/setter bodies allow JVM tests to
exercise model filters without an Android device.

`SwitchTextCell` inherits the app's row inflation and view holder. The extension
overrides the erased `s(RecyclerView$v)` binding method, just as native cells do.
`BaseArcherCell` supplies the remaining Archer lifecycle and visibility methods
in the target app. `RecyclerView$v` is the actual obfuscated ViewHolder name in
this version; it is not an assumption about a generic AndroidX distribution.

Run `tests/smartlife/VerifySmartLife.java` against the supplied original and
patched APKs to check the extension's native method references against the real
DEX definitions. Updating app compatibility requires repeating the APK analysis
and updating these signatures if necessary.
