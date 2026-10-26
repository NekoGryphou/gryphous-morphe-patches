package app.morphe.patches.smartlife.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal val COMPATIBILITY_SMART_LIFE = Compatibility(
    name = "Smart Life",
    packageName = "com.tuya.smartlife",
    targets = listOf(AppTarget(version = "7.12.0")),
)

internal const val EXTENSION = "Lapp/morphe/extension/smartlife/"
internal const val HOOKS = "${EXTENSION}UiHooks;"
